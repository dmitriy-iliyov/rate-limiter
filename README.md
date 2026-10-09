[![codecov](https://codecov.io/gh/dmitriy-iliyov/rate-limiter/graph/badge.svg?token=9YJ5LQ45XF)](https://codecov.io/gh/dmitriy-iliyov/rate-limiter)
[![CI](https://github.com/dmitriy-iliyov/rate-limiter/actions/workflows/ci.yml/badge.svg)](https://github.com/dmitriy-iliyov/rate-limiter/actions/workflows/ci.yml)

## Overview
A Spring Boot starter for request rate limiting backed by Redis. Every limiting decision is made by a single Lua
script, so the check and the update of the counter are atomic even when many application instances share one Redis.

The starter ships the building blocks and leaves the wiring to the application:

| Building block | What it is |
|---|---|
| `LimitingStrategy` | decides whether one more request for a key is allowed |
| `RateLimitRepository` | binds a strategy to one target URL (`DefaultRateLimitRepository`) |
| `RateLimitFilter` | servlet filter that applies the repositories and answers `429 Too Many Requests` |

## Modules
| Module | What it is |
|---|---|
| [`rate-limiter`](rate-limiter) | the starter itself |
| [`rate-limiter-example`](rate-limiter-example) | a Spring Boot application wired as in the [quick start](#quick-start), with a Postman collection |

## Installation
```xml
<dependency>
    <groupId>io.github.dmitriyiliyov.ratelimiter</groupId>
    <artifactId>rate-limiter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Requirements: Java 21, Spring Boot 4, a servlet web application (`spring-boot-starter-web`) and a reachable Redis.
`spring-boot-starter-data-redis` comes with the starter, so the connection is configured with the usual
`spring.data.redis.*` properties.

## What the auto-configuration provides
Only the Lua scripts, one `DefaultRedisScript<Boolean>` bean per strategy:

| Bean name | Script | Used by |
|---|---|---|
| `banningCountBasedScript` | `banning_count_based.lua` | `BanningCountBasedLimitingStrategy` |
| `banningSlidingWindowScript` | `banning_sliding_window.lua` | `BanningSlidingWindowLimitingStrategy` |
| `slidingWindowScript` | `sliding_window.lua` | `SlidingWindowLimitingStrategy` |

All three beans have the same type, so inject them **by name** (`@Qualifier`). Strategies, repositories, the
filter and the `RedisTemplate` are declared by the application. This is intentional, because each endpoint
usually needs its own policy.

## Strategies
| Strategy | Behaviour | Parameters |
|---|---|---|
| `BanningCountBasedLimitingStrategy` | fixed window: counts requests during `observeTime`; once `maxAttemptCount` is exceeded the key is banned for `lockTime` | `maxAttemptCount`, `observeTime`, `lockTime` |
| `BanningSlidingWindowLimitingStrategy` | sliding window of `windowSize`; once `maxAttemptCount` is exceeded the key is banned for `lockTime` | `maxAttemptCount`, `windowSize`, `lockTime` |
| `SlidingWindowLimitingStrategy` | sliding window of `windowSize`; requests over `maxAttemptCount` are rejected, no ban | `maxAttemptCount`, `windowSize` |

Every strategy also takes:
- `redisTemplate`: a `RedisTemplate<String, Boolean>` with `StringRedisSerializer` for keys **and** values. The
  script arguments go through the value serializer, and with the default JDK serializer the script receives
  bytes instead of numbers.
- `script`: the matching script bean from the table above.
- `keyTemplate`: the Redis key with a `%s` placeholder for the client key, e.g. `rate-limit:login:%s`. A template
  without `%s` is rejected at construction, because otherwise all clients would share one counter.

Durations are `java.time.Duration` and must be at least 1 ms, and `maxAttemptCount` must be positive. Invalid
values fail at startup.

## Filters
`IpRateLimitFilter` uses `request.getRemoteAddr()` as the key. To limit by something else (user id, API key),
extend `RateLimitFilter` and implement `extractKey`.

### Behind a reverse proxy or load balancer
Behind a proxy, `getRemoteAddr()` returns the address of the proxy, so all clients would share one limit. Let
Tomcat resolve the client address from `X-Forwarded-For` and keep using `IpRateLimitFilter`:

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

## Quick start
```java
@Configuration
public class RateLimitConfig {

    @Bean
    public RedisTemplate<String, Boolean> rateLimitRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Boolean> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

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

    @Bean
    public RateLimitFilter rateLimitFilter(ObjectMapper mapper, List<RateLimitRepository> repositories) {
        return new IpRateLimitFilter(mapper, repositories);
    }

    // Required: see "Register the filter exactly once" below
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
```

Then add the filter to the Spring Security chain:
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimitFilter rateLimitFilter) {
    return http
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            // ...
            .build();
}
```

Several repositories can be declared to protect different endpoints with different policies. The filter collects
all `RateLimitRepository` beans.

### Register the filter exactly once
> [!IMPORTANT]
> Spring Boot automatically registers **every `Filter` bean** in the servlet container. If `RateLimitFilter` is a
> `@Bean` **and** is also added to the Spring Security chain, it runs twice for each request. Every request is then
> counted twice, and clients are blocked after half of `maxAttemptCount`.
>
> Pick one way to register it:
> - **with Spring Security**: add it to the `SecurityFilterChain` and disable the container registration with a
>   `FilterRegistrationBean` and `setEnabled(false)`, as in the quick start;
> - **without Spring Security**: do not add it to any chain and let the container register it, or use a
>   `FilterRegistrationBean` to set its order;
> - or do not expose the filter as a bean at all, and create it inside the `SecurityFilterChain` method.

## Response
A blocked request is answered with:
```
HTTP/1.1 429 Too Many Requests
Content-Type: application/json

{"message":"Too many requests, try later."}
```
Override `RateLimitFilter.handleBlocked` to change it.

## Run the example
The example limits `GET /api/test-rate-limiter/guarded` to 3 requests per 10 seconds per IP and bans the IP for
15 minutes after that. `GET /api/test-rate-limiter/non-guarded` is not limited.

```bash
docker run -d -p 6379:6379 redis
./mvnw install -DskipTests
java -jar rate-limiter-example/target/rate-limiter-example-1.0.0.jar
```

Redis is expected on `localhost:6379`; override it with `SPRING_DATA_REDIS_HOST` and `SPRING_DATA_REDIS_PORT`.

Or start the example together with Redis in Docker:
```bash
docker compose -f rate-limiter-example/docker-compose.yaml up --build
```
Import [`rate-limiter-example/postman/rate-limiter-example.postman_collection.json`](rate-limiter-example/postman/rate-limiter-example.postman_collection.json)
into Postman to call both endpoints.

## Limitations
- **Redis Cluster is not supported by the banning strategies.** Their scripts touch two keys (`<key>` and
  `blocked:<key>`) that can hash to different slots, and Redis Cluster rejects the script with `CROSSSLOT`.
- **Fail-closed on Redis errors.** If Redis is unavailable, requests to rate-limited endpoints fail with `500`.
  Requests to other endpoints are not affected.
- **One repository per URL.** Registering two repositories with the same `targetUrl` fails at startup.
- **Exact URL match.** `targetUrl` is compared with `request.getRequestURI()` as is, including the context path.
  `/api/login/` or `/api/login;jsessionid=...` are not matched.
- **Limiting by IP does not stop clients with many addresses.** Botnets, rotating proxies and IPv6 clients, which
  usually own a whole `/64`, get a separate limit for every address. Limit by user or API key where it matters.
