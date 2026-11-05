package io.github.dmitriyiliyov.ratelimiter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Servlet filter that applies the {@link RateLimitRepository} registered for the request URI and answers a blocked
 * request with {@code 429 Too Many Requests}. Requests to URIs without a repository pass through untouched.
 * <p>
 * The request URI is looked up after percent-encoding is decoded and {@code ;} path parameters and duplicate slashes
 * are removed, so a client cannot leave the limit by spelling the same path differently.
 * <p>
 * Spring Boot registers every {@code Filter} bean in the servlet container. A filter that is a bean and is also
 * added to the Spring Security chain runs twice per request, and every request is counted twice; disable the
 * container registration with a {@code FilterRegistrationBean} in that case.
 * <p>
 * An exception thrown by a repository, such as a Redis failure, propagates and the request is not passed on.
 * <p>
 * Subclasses decide what identifies a client through {@link #extractKey(HttpServletRequest)}.
 *
 * @see IpRateLimitFilter
 */
@Slf4j
public abstract class RateLimitFilter extends OncePerRequestFilter {

    private static final UrlPathHelper PATH_HELPER = new UrlPathHelper();
    private final ObjectMapper mapper;
    private final Map<String, RateLimitRepository> repositoriesMap;

    /**
     * @param mapper       writes the body of the blocked response
     * @param repositories the repositories of the guarded endpoints
     * @throws IllegalArgumentException if two repositories have the same target URL
     */
    public RateLimitFilter(ObjectMapper mapper, List<RateLimitRepository> repositories) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(repositories, "repositories cannot be null");
        this.mapper = mapper;
        this.repositoriesMap = new HashMap<>();
        repositories.forEach(repository -> {
            if (repositoriesMap.putIfAbsent(repository.getTargetUrl(), repository) != null) {
                throw new IllegalArgumentException("Duplicate rate limit repository for target url: " + repository.getTargetUrl());
            }
        });
    }

    @Override
    protected final void doFilterInternal(@NonNull HttpServletRequest request,
                                          @NonNull HttpServletResponse response,
                                          @NonNull FilterChain filterChain) throws ServletException, IOException {
        RateLimitRepository repository = repositoriesMap.get(PATH_HELPER.getRequestUri(request));
        if (repository != null) {
            String key = extractKey(request);
            if (key == null) {
                throw new IllegalStateException("extractKey returned null for " + repository.getTargetUrl());
            }
            RateLimitStatus status = repository.increment(key);
            if (RateLimitStatus.BLOCKED == status) {
                log.debug("Key {} is blocked", key);
                handleBlocked(response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the key that identifies the client; requests with equal keys share one limit. Called only for
     * requests to a guarded endpoint.
     *
     * @param request the current request
     * @return the client key; a {@code null} is a defect of the subclass and fails the request with
     *         {@link IllegalStateException} instead of letting it past the limit
     */
    protected abstract String extractKey(HttpServletRequest request);

    /**
     * Writes the response to a blocked request; the filter chain is not continued afterwards. Answers
     * {@code 429 Too Many Requests} with the JSON body {@code {"message":"Too many requests, try later."}}.
     *
     * @param response the response to write to
     * @throws IOException if the body cannot be written
     */
    protected void handleBlocked(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(mapper.writeValueAsString(Map.of("message", "Too many requests, try later.")));
    }
}
