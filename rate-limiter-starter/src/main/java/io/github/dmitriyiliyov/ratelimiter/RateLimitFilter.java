package io.github.dmitriyiliyov.ratelimiter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public abstract class RateLimitFilter extends OncePerRequestFilter {

    private static final UrlPathHelper PATH_HELPER = new UrlPathHelper();
    private final ObjectMapper mapper;
    protected final Map<String, RateLimitRepository> repositoriesMap;

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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        RateLimitRepository repository = repositoriesMap.get(PATH_HELPER.getRequestUri(request));
        if (repository != null) {
            String key = extractKey(request);
            RateLimitStatus status = repository.increment(key);
            if (RateLimitStatus.BLOCKED == status) {
                log.debug("Key {} is blocked", key);
                handleBlocked(response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    protected abstract String extractKey(HttpServletRequest request);

    protected void handleBlocked(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(mapper.writeValueAsString(Map.of("message", "Too many requests, try later.")));
    }
}
