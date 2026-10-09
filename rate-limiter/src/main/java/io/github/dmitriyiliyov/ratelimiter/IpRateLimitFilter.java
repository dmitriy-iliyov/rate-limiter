package io.github.dmitriyiliyov.ratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

public class IpRateLimitFilter extends RateLimitFilter {

    public IpRateLimitFilter(ObjectMapper mapper, List<RateLimitRepository> repositories) {
        super(mapper, repositories);
    }

    @Override
    protected String extractKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
