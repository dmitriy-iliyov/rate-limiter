package io.github.dmitriyiliyov.ratelimiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SlidingWindowLimitingStrategyIntegrationTest extends RedisContainerSupport {

    static final String KEY_TEMPLATE = "rate-limit:ip:%s";
    static final String IP = "192.168.0.1";
    static final long MAX_ATTEMPT_COUNT = 3L;
    static final Duration WINDOW_SIZE = Duration.ofSeconds(10);

    @Test
    @DisplayName("IT limit() when requests within limit should return ALLOWED for all")
    void limit_whenRequestsWithinLimit_shouldReturnAllowedForAll() {
        // given
        SlidingWindowLimitingStrategy tested = strategy(WINDOW_SIZE);

        // when / then
        for (int i = 0; i < MAX_ATTEMPT_COUNT; i++) {
            assertThat(tested.limit(IP)).isEqualTo(RateLimitStatus.ALLOWED);
        }
    }

    @Test
    @DisplayName("IT limit() when request exceeds limit within window should return BLOCKED")
    void limit_whenRequestExceedsLimitWithinWindow_shouldReturnBlocked() {
        // given
        SlidingWindowLimitingStrategy tested = strategy(WINDOW_SIZE);
        exhaustLimit(tested);

        // when
        RateLimitStatus status = tested.limit(IP);

        // then
        assertThat(status).isEqualTo(RateLimitStatus.BLOCKED);
    }

    @Test
    @DisplayName("IT limit() when request is allowed should keep window key no longer than window size")
    void limit_whenRequestIsAllowed_shouldKeepWindowKeyNoLongerThanWindowSize() {
        // given
        SlidingWindowLimitingStrategy tested = strategy(WINDOW_SIZE);

        // when
        tested.limit(IP);

        // then
        assertThat(REDIS_TEMPLATE.getExpire(KEY_TEMPLATE.formatted(IP), TimeUnit.MILLISECONDS))
                .isGreaterThan(WINDOW_SIZE.toMillis() / 2)
                .isLessThanOrEqualTo(WINDOW_SIZE.toMillis());
    }

    @Test
    @DisplayName("IT limit() when window has passed should return ALLOWED again")
    void limit_whenWindowHasPassed_shouldReturnAllowedAgain() {
        // given
        SlidingWindowLimitingStrategy tested = strategy(Duration.ofMillis(500));
        exhaustLimit(tested);
        await().atMost(Duration.ofSeconds(2)).until(() -> !REDIS_TEMPLATE.hasKey(KEY_TEMPLATE.formatted(IP)));

        // when
        RateLimitStatus status = tested.limit(IP);

        // then
        assertThat(status).isEqualTo(RateLimitStatus.ALLOWED);
    }

    @Test
    @DisplayName("IT limit() when different keys should track windows independently")
    void limit_whenDifferentKeys_shouldTrackWindowsIndependently() {
        // given
        SlidingWindowLimitingStrategy tested = strategy(WINDOW_SIZE);
        exhaustLimit(tested);

        // when
        RateLimitStatus status = tested.limit("10.0.0.1");

        // then
        assertThat(status).isEqualTo(RateLimitStatus.ALLOWED);
    }

    private static SlidingWindowLimitingStrategy strategy(Duration windowSize) {
        return SlidingWindowLimitingStrategy.builder()
                .redisTemplate(REDIS_TEMPLATE)
                .script(script("sliding_window.lua"))
                .keyTemplate(KEY_TEMPLATE)
                .maxAttemptCount(MAX_ATTEMPT_COUNT)
                .windowSize(windowSize)
                .build();
    }

    private static void exhaustLimit(LimitingStrategy strategy) {
        for (int i = 0; i < MAX_ATTEMPT_COUNT; i++) {
            strategy.limit(IP);
        }
    }
}
