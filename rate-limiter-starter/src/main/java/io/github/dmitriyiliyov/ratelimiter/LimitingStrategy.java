package io.github.dmitriyiliyov.ratelimiter;

public interface LimitingStrategy {
    RateLimitStatus limit(String key);
}
