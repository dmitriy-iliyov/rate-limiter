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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlidingWindowLimitingStrategyUnitTest {

    static final String KEY_TEMPLATE = "rate-limit:ip:%s";

    @Mock
    RedisTemplate<String, Boolean> redisTemplate;

    @Mock
    DefaultRedisScript<Boolean> script;

    @Test
    @DisplayName("UT constructor when redisTemplate is null should throw NullPointerException")
    void constructor_whenRedisTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new SlidingWindowLimitingStrategy(null, script, KEY_TEMPLATE, 3L, Duration.ofSeconds(10)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("redisTemplate must not be null");
    }

    @Test
    @DisplayName("UT constructor when script is null should throw NullPointerException")
    void constructor_whenScriptIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new SlidingWindowLimitingStrategy(redisTemplate, null, KEY_TEMPLATE, 3L, Duration.ofSeconds(10)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("script must not be null");
    }

    @Test
    @DisplayName("UT constructor when keyTemplate has no placeholder should throw IllegalArgumentException")
    void constructor_whenKeyTemplateHasNoPlaceholder_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> new SlidingWindowLimitingStrategy(redisTemplate, script, "rate-limit", 3L, Duration.ofSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT constructor when maxAttemptCount is not positive should throw IllegalArgumentException")
    void constructor_whenMaxAttemptCountIsNotPositive_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> new SlidingWindowLimitingStrategy(redisTemplate, script, KEY_TEMPLATE, 0L, Duration.ofSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT constructor when windowSize is null should throw NullPointerException")
    void constructor_whenWindowSizeIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new SlidingWindowLimitingStrategy(redisTemplate, script, KEY_TEMPLATE, 3L, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("windowSize must not be null");
    }

    @Test
    @DisplayName("UT limit() should pass window key, request id and window size in milliseconds to script")
    void limit_shouldPassWindowKeyRequestIdAndWindowSizeInMillisecondsToScript() {
        // given
        SlidingWindowLimitingStrategy tested = new SlidingWindowLimitingStrategy(
                redisTemplate, script, KEY_TEMPLATE, 3L, Duration.ofSeconds(10)
        );
        when(redisTemplate.execute(
                eq(script),
                eq(List.of("rate-limit:ip:10.0.0.1")),
                anyString(),
                eq("10000"),
                eq("3")
        )).thenReturn(Boolean.TRUE);

        // when
        RateLimitStatus status = tested.limit("10.0.0.1");

        // then
        assertThat(status).isEqualTo(RateLimitStatus.ALLOWED);
    }
}
