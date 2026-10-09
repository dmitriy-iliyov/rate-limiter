package io.github.dmitriyiliyov.ratelimiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitStatusUnitTest {

    @Test
    @DisplayName("UT fromBoolean() when true should return ALLOWED")
    void fromBoolean_whenTrue_shouldReturnAllowed() {
        assertThat(RateLimitStatus.fromBoolean(Boolean.TRUE)).isEqualTo(RateLimitStatus.ALLOWED);
    }

    @Test
    @DisplayName("UT fromBoolean() when false should return BLOCKED")
    void fromBoolean_whenFalse_shouldReturnBlocked() {
        assertThat(RateLimitStatus.fromBoolean(Boolean.FALSE)).isEqualTo(RateLimitStatus.BLOCKED);
    }

    @Test
    @DisplayName("UT fromBoolean() when null should throw NullPointerException")
    void fromBoolean_whenNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> RateLimitStatus.fromBoolean(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("boolean value cannot be null");
    }
}
