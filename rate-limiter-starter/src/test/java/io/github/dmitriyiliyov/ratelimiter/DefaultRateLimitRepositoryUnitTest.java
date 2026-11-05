package io.github.dmitriyiliyov.ratelimiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultRateLimitRepositoryUnitTest {

    static final String TARGET_URL = "/api/guarded";

    @Mock
    LimitingStrategy strategy;

    @Test
    @DisplayName("UT constructor when strategy is null should throw NullPointerException")
    void constructor_whenStrategyIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultRateLimitRepository(null, TARGET_URL))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("strategy must not be null");
    }

    @Test
    @DisplayName("UT constructor when targetUrl is null should throw NullPointerException")
    void constructor_whenTargetUrlIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultRateLimitRepository(strategy, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("targetUrl must not be null");
    }

    @Test
    @DisplayName("UT builder() should create repository with configured strategy and targetUrl")
    void builder_shouldCreateRepositoryWithConfiguredStrategyAndTargetUrl() {
        // given
        when(strategy.limit("10.0.0.1")).thenReturn(RateLimitStatus.BLOCKED);

        // when
        DefaultRateLimitRepository tested = DefaultRateLimitRepository.builder()
                .strategy(strategy)
                .targetUrl(TARGET_URL)
                .build();

        // then
        assertThat(tested.getTargetUrl()).isEqualTo(TARGET_URL);
        assertThat(tested.increment("10.0.0.1")).isEqualTo(RateLimitStatus.BLOCKED);
    }

    @Test
    @DisplayName("UT increment() should return status decided by strategy")
    void increment_shouldReturnStatusDecidedByStrategy() {
        // given
        DefaultRateLimitRepository tested = new DefaultRateLimitRepository(strategy, TARGET_URL);
        when(strategy.limit("10.0.0.1")).thenReturn(RateLimitStatus.ALLOWED);

        // when
        RateLimitStatus status = tested.increment("10.0.0.1");

        // then
        assertThat(status).isEqualTo(RateLimitStatus.ALLOWED);
    }
}
