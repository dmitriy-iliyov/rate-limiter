package io.github.dmitriyiliyov.ratelimiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class BanningSlidingWindowLimitingStrategyIntegrationTest extends RedisContainerSupport {

    static final String KEY_TEMPLATE = "rate-limit:ip:%s";
    static final String IP = "192.168.0.1";
    static final long MAX_ATTEMPT_COUNT = 3L;
    static final Duration WINDOW_SIZE = Duration.ofSeconds(10);
    static final Duration LOCK_TIME = Duration.ofMinutes(15);

    @Test
    @DisplayName("IT limit() when requests within limit should return ALLOWED for all")
    void limit_whenRequestsWithinLimit_shouldReturnAllowedForAll() {
        // given
        BanningSlidingWindowLimitingStrategy tested = strategy();

        // when / then
        for (int i = 0; i < MAX_ATTEMPT_COUNT; i++) {
            assertThat(tested.limit(IP)).isEqualTo(RateLimitStatus.ALLOWED);
        }
    }

    @Test
    @DisplayName("IT limit() when request exceeds limit should return BLOCKED and set blocked key with lock TTL")
    void limit_whenRequestExceedsLimit_shouldReturnBlockedAndSetBlockedKeyWithLockTtl() {
        // given
        BanningSlidingWindowLimitingStrategy tested = strategy();
        exhaustLimit(tested);

        // when
        RateLimitStatus status = tested.limit(IP);

        // then
        assertThat(status).isEqualTo(RateLimitStatus.BLOCKED);
        assertThat(REDIS_TEMPLATE.getExpire("blocked:" + KEY_TEMPLATE.formatted(IP), TimeUnit.MILLISECONDS))
                .isGreaterThan(WINDOW_SIZE.toMillis())
                .isLessThanOrEqualTo(LOCK_TIME.toMillis());
    }

    @Test
    @DisplayName("IT limit() when key is already blocked should return BLOCKED")
    void limit_whenKeyIsAlreadyBlocked_shouldReturnBlocked() {
        // given
        BanningSlidingWindowLimitingStrategy tested = strategy();
        exhaustLimit(tested);
        tested.limit(IP);

        // when
        RateLimitStatus status = tested.limit(IP);

        // then
        assertThat(status).isEqualTo(RateLimitStatus.BLOCKED);
    }

    @Test
    @DisplayName("IT limit() when different keys should track windows independently")
    void limit_whenDifferentKeys_shouldTrackWindowsIndependently() {
        // given
        BanningSlidingWindowLimitingStrategy tested = strategy();
        exhaustLimit(tested);
        tested.limit(IP);

        // when
        RateLimitStatus status = tested.limit("10.0.0.1");

        // then
        assertThat(status).isEqualTo(RateLimitStatus.ALLOWED);
    }

    private static BanningSlidingWindowLimitingStrategy strategy() {
        return BanningSlidingWindowLimitingStrategy.builder()
                .redisTemplate(REDIS_TEMPLATE)
                .script(script("banning_sliding_window.lua"))
                .keyTemplate(KEY_TEMPLATE)
                .maxAttemptCount(MAX_ATTEMPT_COUNT)
                .windowSize(WINDOW_SIZE)
                .lockTime(LOCK_TIME)
                .build();
    }

    private static void exhaustLimit(LimitingStrategy strategy) {
        for (int i = 0; i < MAX_ATTEMPT_COUNT; i++) {
            strategy.limit(IP);
        }
    }
}
