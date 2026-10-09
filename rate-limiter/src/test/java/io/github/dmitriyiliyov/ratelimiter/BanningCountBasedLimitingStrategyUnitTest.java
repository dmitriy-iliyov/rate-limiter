package io.github.dmitriyiliyov.ratelimiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BanningCountBasedLimitingStrategyUnitTest {

    static final String KEY_TEMPLATE = "rate-limit:ip:%s";

    @Mock
    RedisTemplate<String, Boolean> redisTemplate;

    @Mock
    DefaultRedisScript<Boolean> script;

    @Test
    @DisplayName("UT constructor when redisTemplate is null should throw NullPointerException")
    void constructor_whenRedisTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new BanningCountBasedLimitingStrategy(
                null, script, KEY_TEMPLATE, 3L, Duration.ofSeconds(10), Duration.ofMinutes(15)
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("redisTemplate must not be null");
    }

    @Test
    @DisplayName("UT constructor when script is null should throw NullPointerException")
    void constructor_whenScriptIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new BanningCountBasedLimitingStrategy(
                redisTemplate, null, KEY_TEMPLATE, 3L, Duration.ofSeconds(10), Duration.ofMinutes(15)
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("script must not be null");
    }

    @Test
    @DisplayName("UT constructor when keyTemplate has no placeholder should throw IllegalArgumentException")
    void constructor_whenKeyTemplateHasNoPlaceholder_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> new BanningCountBasedLimitingStrategy(
                redisTemplate, script, "rate-limit", 3L, Duration.ofSeconds(10), Duration.ofMinutes(15)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT constructor when maxAttemptCount is not positive should throw IllegalArgumentException")
    void constructor_whenMaxAttemptCountIsNotPositive_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> new BanningCountBasedLimitingStrategy(
                redisTemplate, script, KEY_TEMPLATE, 0L, Duration.ofSeconds(10), Duration.ofMinutes(15)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT constructor when observeTime is null should throw NullPointerException")
    void constructor_whenObserveTimeIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new BanningCountBasedLimitingStrategy(
                redisTemplate, script, KEY_TEMPLATE, 3L, null, Duration.ofMinutes(15)
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("observeTime must not be null");
    }

    @Test
    @DisplayName("UT constructor when lockTime is zero should throw IllegalArgumentException")
    void constructor_whenLockTimeIsZero_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> new BanningCountBasedLimitingStrategy(
                redisTemplate, script, KEY_TEMPLATE, 3L, Duration.ofSeconds(10), Duration.ZERO
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT limit() should pass counter and blocked keys and durations in milliseconds to script")
    void limit_shouldPassCounterAndBlockedKeysAndDurationsInMillisecondsToScript() {
        // given
        BanningCountBasedLimitingStrategy tested = new BanningCountBasedLimitingStrategy(
                redisTemplate, script, KEY_TEMPLATE, 3L, Duration.ofSeconds(10), Duration.ofMinutes(15)
        );
        when(redisTemplate.execute(
                script,
                List.of("rate-limit:ip:10.0.0.1", "blocked:rate-limit:ip:10.0.0.1"),
                "10000",
                "900000",
                "3"
        )).thenReturn(Boolean.TRUE);

        // when
        RateLimitStatus status = tested.limit("10.0.0.1");

        // then
        assertThat(status).isEqualTo(RateLimitStatus.ALLOWED);
    }

    @Test
    @DisplayName("UT limit() when script answers false should return BLOCKED")
    void limit_whenScriptAnswersFalse_shouldReturnBlocked() {
        // given
        BanningCountBasedLimitingStrategy tested = new BanningCountBasedLimitingStrategy(
                redisTemplate, script, KEY_TEMPLATE, 3L, Duration.ofSeconds(10), Duration.ofMinutes(15)
        );
        when(redisTemplate.execute(
                script,
                List.of("rate-limit:ip:10.0.0.1", "blocked:rate-limit:ip:10.0.0.1"),
                "10000",
                "900000",
                "3"
        )).thenReturn(Boolean.FALSE);

        // when
        RateLimitStatus status = tested.limit("10.0.0.1");

        // then
        assertThat(status).isEqualTo(RateLimitStatus.BLOCKED);
    }
}
