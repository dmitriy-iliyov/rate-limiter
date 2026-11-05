package io.github.dmitriyiliyov.ratelimiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class BanningCountBasedLimitingStrategyIntegrationTest extends RedisContainerSupport {

    static final String KEY_TEMPLATE = "rate-limit:ip:%s";
    static final String IP = "192.168.0.1";
    static final long MAX_ATTEMPT_COUNT = 3L;
    static final Duration OBSERVE_TIME = Duration.ofSeconds(10);
    static final Duration LOCK_TIME = Duration.ofMinutes(15);

    @Test
    @DisplayName("IT limit() when first request should return ALLOWED and set observe TTL on counter key")
    void limit_whenFirstRequest_shouldReturnAllowedAndSetObserveTtlOnCounterKey() {
        // given
        BanningCountBasedLimitingStrategy tested = strategy(OBSERVE_TIME);

        // when
        RateLimitStatus status = tested.limit(IP);

        // then
        assertThat(status).isEqualTo(RateLimitStatus.ALLOWED);
        assertThat(REDIS_TEMPLATE.getExpire(counterKey(IP), TimeUnit.MILLISECONDS))
                .isPositive()
                .isLessThanOrEqualTo(OBSERVE_TIME.toMillis());
    }

    @Test
    @DisplayName("IT limit() when requests within limit should return ALLOWED for all")
    void limit_whenRequestsWithinLimit_shouldReturnAllowedForAll() {
        // given
        BanningCountBasedLimitingStrategy tested = strategy(OBSERVE_TIME);

        // when / then
        for (int i = 0; i < MAX_ATTEMPT_COUNT; i++) {
            assertThat(tested.limit(IP)).isEqualTo(RateLimitStatus.ALLOWED);
        }
    }

    @Test
    @DisplayName("IT limit() when request exceeds limit should return BLOCKED and set blocked key with lock TTL")
    void limit_whenRequestExceedsLimit_shouldReturnBlockedAndSetBlockedKeyWithLockTtl() {
        // given
        BanningCountBasedLimitingStrategy tested = strategy(OBSERVE_TIME);
        exhaustLimit(tested);

        // when
        RateLimitStatus status = tested.limit(IP);

        // then
        assertThat(status).isEqualTo(RateLimitStatus.BLOCKED);
        assertThat(REDIS_TEMPLATE.getExpire(blockedKey(IP), TimeUnit.MILLISECONDS))
                .isGreaterThan(OBSERVE_TIME.toMillis())
                .isLessThanOrEqualTo(LOCK_TIME.toMillis());
    }

    @Test
    @DisplayName("IT limit() when key is already blocked should return BLOCKED")
    void limit_whenKeyIsAlreadyBlocked_shouldReturnBlocked() {
        // given
        BanningCountBasedLimitingStrategy tested = strategy(OBSERVE_TIME);
        exhaustLimit(tested);
        tested.limit(IP);

        // when
        RateLimitStatus status = tested.limit(IP);

        // then
        assertThat(status).isEqualTo(RateLimitStatus.BLOCKED);
    }

    @Test
    @DisplayName("IT limit() when observe time expires should reset counter and return ALLOWED")
    void limit_whenObserveTimeExpires_shouldResetCounterAndReturnAllowed() {
        // given
        BanningCountBasedLimitingStrategy tested = strategy(Duration.ofMillis(500));
        exhaustLimit(tested);
        await().atMost(Duration.ofSeconds(2)).until(() -> !REDIS_TEMPLATE.hasKey(counterKey(IP)));

        // when
        RateLimitStatus status = tested.limit(IP);

        // then
        assertThat(status).isEqualTo(RateLimitStatus.ALLOWED);
    }

    @Test
    @DisplayName("IT limit() when different keys should track counters independently")
    void limit_whenDifferentKeys_shouldTrackCountersIndependently() {
        // given
        BanningCountBasedLimitingStrategy tested = strategy(OBSERVE_TIME);
        exhaustLimit(tested);
        tested.limit(IP);

        // when
        RateLimitStatus status = tested.limit("10.0.0.1");

        // then
        assertThat(status).isEqualTo(RateLimitStatus.ALLOWED);
    }

    private static BanningCountBasedLimitingStrategy strategy(Duration observeTime) {
        return BanningCountBasedLimitingStrategy.builder()
                .redisTemplate(REDIS_TEMPLATE)
                .script(script("banning_count_based.lua"))
                .keyTemplate(KEY_TEMPLATE)
                .maxAttemptCount(MAX_ATTEMPT_COUNT)
                .observeTime(observeTime)
                .lockTime(LOCK_TIME)
                .build();
    }

    private static void exhaustLimit(LimitingStrategy strategy) {
        for (int i = 0; i < MAX_ATTEMPT_COUNT; i++) {
            strategy.limit(IP);
        }
    }

    private static String counterKey(String ip) {
        return KEY_TEMPLATE.formatted(ip);
    }

    private static String blockedKey(String ip) {
        return "blocked:" + KEY_TEMPLATE.formatted(ip);
    }
}
