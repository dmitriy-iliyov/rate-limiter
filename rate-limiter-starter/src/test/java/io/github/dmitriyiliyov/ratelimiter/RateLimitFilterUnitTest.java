package io.github.dmitriyiliyov.ratelimiter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterUnitTest {

    @Mock
    ObjectMapper mapper;

    @Mock
    RateLimitRepository repository;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain filterChain;

    @Mock
    PrintWriter writer;

    RateLimitFilter tested;

    String targetUrl;
    String ip;

    @BeforeEach
    void setUp() {
        targetUrl = "/api/guarded";
        ip = "192.168.0.1";
        lenient().when(repository.getTargetUrl()).thenReturn(targetUrl);
        tested = new RateLimitFilter(mapper, List.of(repository)) {
            @Override
            protected String extractKey(HttpServletRequest request) {
                return ip;
            }
        };
    }

    @Test
    @DisplayName("UT constructor when mapper is null should throw NullPointerException")
    void constructor_whenMapperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new RateLimitFilter(null, List.of(repository)) {
            @Override
            protected String extractKey(HttpServletRequest request) {
                return ip;
            }
        })
                .isInstanceOf(NullPointerException.class)
                .hasMessage("mapper cannot be null");
    }

    @Test
    @DisplayName("UT constructor when repositories is null should throw NullPointerException")
    void constructor_whenRepositoriesIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new RateLimitFilter(mapper, null) {
            @Override
            protected String extractKey(HttpServletRequest request) {
                return ip;
            }
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("UT constructor when two repositories have the same targetUrl should throw IllegalArgumentException")
    void constructor_whenRepositoriesHaveSameTargetUrl_shouldThrowIllegalArgumentException(@Mock RateLimitRepository another) {
        // given
        when(another.getTargetUrl()).thenReturn(targetUrl);

        // when & then
        assertThatThrownBy(() -> new RateLimitFilter(mapper, List.of(repository, another)) {
            @Override
            protected String extractKey(HttpServletRequest request) {
                return ip;
            }
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT doFilterInternal() when URI not in repositories map should pass to filterChain")
    void doFilterInternal_whenUriNotInRepositoriesMap_shouldPassToFilterChain() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/not/rate/limited");

        // when
        tested.doFilterInternal(request, response, filterChain);

        // then
        verify(repository, times(1)).getTargetUrl();
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("UT doFilterInternal() when URI matches and status is ALLOWED should pass to filterChain")
    void doFilterInternal_whenUriMatchesAndStatusIsAllowed_shouldPassToFilterChain() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn(targetUrl);
        when(repository.increment(ip)).thenReturn(RateLimitStatus.ALLOWED);

        // when
        tested.doFilterInternal(request, response, filterChain);

        // then
        verify(repository, times(1)).getTargetUrl();
        verify(repository, times(1)).increment(ip);
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("UT doFilterInternal() when URI matches and status is BLOCKED should respond 429 and not pass to filterChain")
    void doFilterInternal_whenUriMatchesAndStatusIsBlocked_shouldRespond429AndNotPassToFilterChain() throws ServletException, IOException {
        // given
        String expectedJson = "{\"message\":\"Too many requests, try later.\"}";
        when(request.getRequestURI()).thenReturn(targetUrl);
        when(repository.increment(ip)).thenReturn(RateLimitStatus.BLOCKED);
        when(response.getWriter()).thenReturn(writer);
        when(mapper.writeValueAsString(any())).thenReturn(expectedJson);

        // when
        tested.doFilterInternal(request, response, filterChain);

        // then
        verify(repository, times(1)).getTargetUrl();
        verify(repository, times(1)).increment(ip);
        verify(response, times(1)).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response, times(1)).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(writer, times(1)).write(expectedJson);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(filterChain);
    }
}