[![CodeFactor](https://www.codefactor.io/repository/github/dmitriy-iliyov/rate-limiter/badge)](https://www.codefactor.io/repository/github/dmitriy-iliyov/rate-limiter)
[![codecov](https://codecov.io/gh/dmitriy-iliyov/rate-limiter/graph/badge.svg?token=9YJ5LQ45XF)](https://codecov.io/gh/dmitriy-iliyov/rate-limiter)
[![CI](https://github.com/dmitriy-iliyov/rate-limiter/actions/workflows/ci.yml/badge.svg)](https://github.com/dmitriy-iliyov/rate-limiter/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.dmitriy-iliyov/rate-limiter-starter.svg?label=maven-central&color=blue)](https://central.sonatype.com/artifact/io.github.dmitriy-iliyov/rate-limiter-starter)
[![javadoc](https://javadoc.io/badge2/io.github.dmitriy-iliyov/rate-limiter-starter/javadoc.svg)](https://javadoc.io/doc/io.github.dmitriy-iliyov/rate-limiter-starter)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Release](https://img.shields.io/github/release/dmitriy-iliyov/rate-limiter)
![GitHub last commit](https://img.shields.io/github/last-commit/dmitriy-iliyov/rate-limiter)

## Overview
A Spring Boot starter for request rate limiting backed by Redis. Every limiting decision is made by a single Lua
script, so the check and the update of the counter are atomic even when many application instances share one Redis.

The starter ships the building blocks and leaves the wiring to the application: each endpoint usually needs its own
policy, so strategies, repositories and the filter are declared as your own beans.

## Key Features
- **Atomic decisions** - the check and the increment run as one Lua script, with no race between instances.
- **Three strategies** - fixed window with a ban, sliding window with a ban, and a plain sliding window.
- **Per-endpoint policies** - every protected URL gets its own strategy, limits and Redis key space.
- **Pluggable client key** - limits by IP out of the box; extend the filter to limit by user id or API key.
- **Proxy-safe client IP** - works behind a reverse proxy through Tomcat's `RemoteIpValve`, without trusting
  addresses the client wrote itself.
- **Fail-fast configuration** - invalid durations, limits and key templates are rejected at startup.

## Supported Infrastructure
- **Java:** 21+.
- **Spring Boot:** 4.x, servlet web applications.
- **Storage:** Redis 7+ (standalone or Sentinel).

## Limitations
- **Redis Cluster is not supported by the banning strategies** - their scripts touch two keys (`<key>` and
  `blocked:<key>`) that can hash to different slots, and Redis Cluster rejects the script with `CROSSSLOT`.
- **Fail-closed on Redis errors** - if Redis is unavailable, requests to rate-limited endpoints fail with `500`.
  Requests to other endpoints are not affected.
- **One repository per URL** - registering two repositories with the same `targetUrl` fails at startup.
- **Exact path match** - `targetUrl` is compared with the request URI including the context path, after
  percent-encoding is decoded and `;` path parameters and duplicate slashes are removed, so `/api/%6Cogin`,
  `/api/login;jsessionid=...` and `/api//login` all count against `/api/login`. Patterns are not supported, and
  `/api/login/` is a different URL.
- **Limiting by IP does not stop clients with many addresses** - botnets, rotating proxies and IPv6 clients, which
  usually own a whole `/64`, get a separate limit for every address. Limit by user or API key where it matters.

## Quick Start

1. Add dependency
```xml
  <dependency>
      <groupId>io.github.dmitriy-iliyov</groupId>
      <artifactId>rate-limiter-starter</artifactId>
      <version>1.0.0</version>
  </dependency>
```
`spring-boot-starter-data-redis` comes with the starter, so the connection is configured with the usual
`spring.data.redis.*` properties.

2. Declare a `RedisTemplate` for the strategies
```java
@Bean
public RedisTemplate<String, Boolean> rateLimitRedisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Boolean> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    return template;
}
```
> [!IMPORTANT]
> Use `StringRedisSerializer` for keys **and** values. The script arguments go through the value serializer, and
> with the default JDK serializer the script receives bytes instead of numbers.

3. Declare a repository for every endpoint to protect
```java
@Bean
public RateLimitRepository loginRateLimitRepository(RedisTemplate<String, Boolean> rateLimitRedisTemplate,
                                                    @Qualifier("banningCountBasedScript")
                                                    DefaultRedisScript<Boolean> script) {
    LimitingStrategy strategy = BanningCountBasedLimitingStrategy.builder()
            .redisTemplate(rateLimitRedisTemplate)
            .script(script)
            .keyTemplate("rate-limit:login:%s")
            .maxAttemptCount(3)
            .observeTime(Duration.ofSeconds(10))
            .lockTime(Duration.ofMinutes(15))
            .build();
    return DefaultRateLimitRepository.builder()
            .strategy(strategy)
            .targetUrl("/api/login")
            .build();
}
```

4. Declare the filter and add it to the Spring Security chain
```java
@Bean
public RateLimitFilter rateLimitFilter(ObjectMapper mapper, List<RateLimitRepository> repositories) {
    return new IpRateLimitFilter(mapper, repositories);
}

@Bean
public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
    FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
}

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimitFilter rateLimitFilter) {
    return http
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            // ...
            .build();
}
```
The `FilterRegistrationBean` is required, read why [here](#filter-registration).

---

## Example & Test Environment

Example of a configured application with a Postman collection is [here](https://github.com/dmitriy-iliyov/rate-limiter/tree/main/rate-limiter-example). Project is fully containerised with Docker.

## Design

### Architecture Overview
| Building block | What it is |
|---|---|
| `LimitingStrategy` | decides whether one more request for a key is allowed |
| `RateLimitRepository` | binds a strategy to one target URL (`DefaultRateLimitRepository`) |
| `RateLimitFilter` | servlet filter that finds the repository by request URI, extracts the client key and answers `429 Too Many Requests` |

The filter collects all `RateLimitRepository` beans, so several endpoints are protected with different policies
by declaring several repositories.

### Lua Scripts
The auto-configuration registers only the Lua scripts, one `DefaultRedisScript<Boolean>` bean per strategy:

| Bean name | Script | Used by |
|---|---|---|
| `banningCountBasedScript` | `banning_count_based.lua` | `BanningCountBasedLimitingStrategy` |
| `banningSlidingWindowScript` | `banning_sliding_window.lua` | `BanningSlidingWindowLimitingStrategy` |
| `slidingWindowScript` | `sliding_window.lua` | `SlidingWindowLimitingStrategy` |

All three beans have the same type, so inject them **by name** (`@Qualifier`). To replace a script, declare your
own `DefaultRedisScript<Boolean>` bean with the same name; the other two scripts are still registered.

### Strategies
| Strategy | Behaviour | Parameters |
|---|---|---|
| `BanningCountBasedLimitingStrategy` | fixed window: counts requests during `observeTime`; once `maxAttemptCount` is exceeded the key is banned for `lockTime` | `maxAttemptCount`, `observeTime`, `lockTime` |
| `BanningSlidingWindowLimitingStrategy` | sliding window of `windowSize`; once `maxAttemptCount` is exceeded the key is banned for `lockTime` | `maxAttemptCount`, `windowSize`, `lockTime` |
| `SlidingWindowLimitingStrategy` | sliding window of `windowSize`; requests over `maxAttemptCount` are rejected, no ban | `maxAttemptCount`, `windowSize` |

Every strategy also takes the `redisTemplate`, the matching `script` and a `keyTemplate` - the Redis key with a `%s`
placeholder for the client key, e.g. `rate-limit:login:%s`. A template without `%s` is rejected, because otherwise
all clients would share one counter. Durations must be at least 1 ms and `maxAttemptCount` must be positive.

### Client Key
`IpRateLimitFilter` uses `request.getRemoteAddr()` as the key. To limit by something else (user id, API key),
extend `RateLimitFilter` and implement `extractKey`.
`extractKey` must not return `null`: a `null` key is treated as a bug in the subclass, and the request fails with
`IllegalStateException` (`500`) instead of passing the limit.

Behind a reverse proxy or load balancer, `getRemoteAddr()` returns the address of the proxy, so all clients would
share one limit. Let Tomcat resolve the client address from `X-Forwarded-For` and keep using `IpRateLimitFilter`:
```properties
server.forward-headers-strategy=native
# regex of YOUR proxies' addresses; the default trusts every private network
server.tomcat.remoteip.internal-proxies=10\\.0\\.0\\.2
```
Tomcat reads `X-Forwarded-For` from right to left, skips the trusted proxies and puts the first untrusted address
into `getRemoteAddr()`. Whatever a client writes into the header itself stays to the left of that address and is
ignored.

> [!WARNING]
> Do not take the first address of `X-Forwarded-For` as the client: any client sets it to a new value on every
> request and escapes the limit. `server.forward-headers-strategy=framework` (Spring's `ForwardedHeaderFilter`)
> does exactly that, so use `native`. Keep `internal-proxies` limited to your own proxies: an address trusted by
> mistake, such as another pod in the same private network, can forge the header too.

### Filter Registration
> [!IMPORTANT]
> Spring Boot automatically registers **every `Filter` bean** in the servlet container. If `RateLimitFilter` is a
> `@Bean` **and** is also added to the Spring Security chain, it runs twice for each request. Every request is then
> counted twice, and clients are blocked after half of `maxAttemptCount`.

Pick one way to register it:
- **with Spring Security** - add it to the `SecurityFilterChain` and disable the container registration with a
  `FilterRegistrationBean` and `setEnabled(false)`, as in the [quick start](#quick-start);
- **without Spring Security** - do not add it to any chain and let the container register it, or use a
  `FilterRegistrationBean` to set its order;
- or do not expose the filter as a bean at all, and create it inside the `SecurityFilterChain` method.

### Blocked Response
A blocked request is answered with:
```
HTTP/1.1 429 Too Many Requests
Content-Type: application/json

{"message":"Too many requests, try later."}
```
Override `RateLimitFilter.handleBlocked` to change it.
