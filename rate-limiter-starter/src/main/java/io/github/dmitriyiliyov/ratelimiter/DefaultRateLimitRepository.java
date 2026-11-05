package io.github.dmitriyiliyov.ratelimiter;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * A {@link RateLimitRepository} that applies one {@link LimitingStrategy} to one target URL.
 */
@Slf4j
@Builder
public class DefaultRateLimitRepository implements RateLimitRepository {

    private final LimitingStrategy strategy;
    private final String targetUrl;

    public DefaultRateLimitRepository(LimitingStrategy strategy,
                                      String targetUrl) {
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
        this.targetUrl = Objects.requireNonNull(targetUrl, "targetUrl must not be null");
        log.debug("Create DefaultRateLimitRepository with target url: {}", this.targetUrl);
    }

    @Override
    public String getTargetUrl() {
        return targetUrl;
    }

    @Override
    public RateLimitStatus increment(String key) {
        return strategy.limit(key);
    }
}
