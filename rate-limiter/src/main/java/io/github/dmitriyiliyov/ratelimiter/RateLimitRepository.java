package io.github.dmitriyiliyov.ratelimiter;

public interface RateLimitRepository {
    String getTargetUrl();
    RateLimitStatus increment(String key);
}
