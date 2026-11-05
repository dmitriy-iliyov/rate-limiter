package io.github.dmitriyiliyov.ratelimiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitingStrategyUtilsUnitTest {

    @Test
    @DisplayName("UT requireKeyTemplate() when template contains placeholder should return it")
    void requireKeyTemplate_whenTemplateContainsPlaceholder_shouldReturnIt() {
        assertThat(LimitingStrategyUtils.requireKeyTemplate("rate-limit:ip:%s")).isEqualTo("rate-limit:ip:%s");
    }

    @Test
    @DisplayName("UT requireKeyTemplate() when template is null should throw NullPointerException")
    void requireKeyTemplate_whenTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> LimitingStrategyUtils.requireKeyTemplate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("keyTemplate must not be null");
    }

    @Test
    @DisplayName("UT requireKeyTemplate() when template has no placeholder should throw IllegalArgumentException")
    void requireKeyTemplate_whenTemplateHasNoPlaceholder_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> LimitingStrategyUtils.requireKeyTemplate("rate-limit"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("keyTemplate must contain '%s' placeholder for the key, got: rate-limit");
    }

    @Test
    @DisplayName("UT requireKeyTemplate() when template is empty should throw IllegalArgumentException")
    void requireKeyTemplate_whenTemplateIsEmpty_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> LimitingStrategyUtils.requireKeyTemplate(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT requirePositive() when value is positive should return it")
    void requirePositive_whenValueIsPositive_shouldReturnIt() {
        assertThat(LimitingStrategyUtils.requirePositive(1L, "maxAttemptCount")).isEqualTo(1L);
    }

    @Test
    @DisplayName("UT requirePositive() when value is zero should throw IllegalArgumentException")
    void requirePositive_whenValueIsZero_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> LimitingStrategyUtils.requirePositive(0L, "maxAttemptCount"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxAttemptCount must be positive, got: 0");
    }

    @Test
    @DisplayName("UT requirePositive() when value is negative should throw IllegalArgumentException")
    void requirePositive_whenValueIsNegative_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> LimitingStrategyUtils.requirePositive(-1L, "maxAttemptCount"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT requirePositiveMillis() when duration is at least one millisecond should return it")
    void requirePositiveMillis_whenDurationIsAtLeastOneMillisecond_shouldReturnIt() {
        assertThat(LimitingStrategyUtils.requirePositiveMillis(Duration.ofMillis(1), "lockTime"))
                .isEqualTo(Duration.ofMillis(1));
    }

    @Test
    @DisplayName("UT requirePositiveMillis() when duration is null should throw NullPointerException")
    void requirePositiveMillis_whenDurationIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> LimitingStrategyUtils.requirePositiveMillis(null, "lockTime"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("lockTime must not be null");
    }

    @Test
    @DisplayName("UT requirePositiveMillis() when duration is zero should throw IllegalArgumentException")
    void requirePositiveMillis_whenDurationIsZero_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> LimitingStrategyUtils.requirePositiveMillis(Duration.ZERO, "lockTime"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("lockTime must be at least 1 ms, got: PT0S");
    }

    @Test
    @DisplayName("UT requirePositiveMillis() when duration is shorter than a millisecond should throw IllegalArgumentException")
    void requirePositiveMillis_whenDurationIsShorterThanMillisecond_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> LimitingStrategyUtils.requirePositiveMillis(Duration.ofNanos(999_999), "lockTime"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT requirePositiveMillis() when duration is negative should throw IllegalArgumentException")
    void requirePositiveMillis_whenDurationIsNegative_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> LimitingStrategyUtils.requirePositiveMillis(Duration.ofSeconds(-1), "lockTime"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
