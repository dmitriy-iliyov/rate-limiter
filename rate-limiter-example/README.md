![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-7-6DB33F?logo=springsecurity)
![Redis](https://img.shields.io/badge/Redis-7+-DC382D?logo=redis)

## Overview

A demo project that showcases the [rate-limiter](https://github.com/dmitriy-iliyov/rate-limiter) starter usage in a
Spring Boot application protected by Spring Security.

The project demonstrates:
- limiting one endpoint by client IP while leaving another one unlimited
- a ban of the client after the limit is exceeded
- registering the filter in the Spring Security chain exactly once

## Architecture Overview

| Endpoint | Policy |
|---|---|
| `GET /api/test-rate-limiter/guarded` | `BanningCountBasedLimitingStrategy`: 3 requests per 10 seconds per IP, then the IP is banned for 15 minutes |
| `GET /api/test-rate-limiter/non-guarded` | not limited |

- [`RateLimiterConfig`](src/main/java/io/github/dmitriyiliyov/ratelimiter/example/RateLimiterConfig.java) - the
  `RedisTemplate`, the repository of the guarded endpoint, `IpRateLimitFilter` and its disabled container registration
- [`SecurityConfig`](src/main/java/io/github/dmitriyiliyov/ratelimiter/example/SecurityConfig.java) - adds the filter
  before `UsernamePasswordAuthenticationFilter` and permits every request

## Ports

| Service     | Port |
|-------------|------|
| Example API | 8080 |
| Redis       | 6379 |

Redis is expected on `localhost:6379`; override it with `SPRING_DATA_REDIS_HOST` and `SPRING_DATA_REDIS_PORT`.

## Typical Flow

1. The first 3 requests to `/guarded` within 10 seconds are answered with `200`
2. The 4th request is answered with `429 Too Many Requests` and the IP is banned
3. Every request to `/guarded` from that IP is answered with `429` for the next 15 minutes
4. `/non-guarded` keeps answering `200` all the time

## Run

### Docker
From the `rate-limiter-example` directory (the build context is the repository root, so the starter itself is
built from source):
```bash
docker compose up --build
```

### Locally
From the repository root:
```bash
docker run -d -p 6379:6379 redis:7-alpine
./mvnw install -DskipTests -Dgpg.skip=true
./mvnw -f rate-limiter-example/pom.xml package
java -jar rate-limiter-example/target/rate-limiter-example-1.0.1.jar
```

## Postman

Import [`postman/rate-limiter-example.postman_collection.json`](postman/rate-limiter-example.postman_collection.json)
into Postman to call both endpoints.
