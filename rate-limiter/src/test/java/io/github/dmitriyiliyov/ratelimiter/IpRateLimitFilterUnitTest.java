package io.github.dmitriyiliyov.ratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpRateLimitFilterUnitTest {

    @Mock
    ObjectMapper mapper;

    @Mock
    HttpServletRequest request;

    @Test
    @DisplayName("UT extractKey() should return remoteAddr and ignore X-Forwarded-For")
    void extractKey_shouldReturnRemoteAddrAndIgnoreXForwardedFor() {
        // given
        IpRateLimitFilter tested = new IpRateLimitFilter(mapper, List.of());
        when(request.getRemoteAddr()).thenReturn("192.168.0.1");

        // when
        String key = tested.extractKey(request);

        // then
        assertThat(key).isEqualTo("192.168.0.1");
        verify(request, never()).getHeader("X-Forwarded-For");
    }
}
