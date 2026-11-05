package io.github.dmitriyiliyov.ratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * A {@link RateLimitFilter} that limits clients by {@link HttpServletRequest#getRemoteAddr()}.
 * <p>
 * Behind a reverse proxy the remote address is the address of the proxy, and all clients share one limit unless
 * Tomcat resolves the client address from {@code X-Forwarded-For} ({@code server.forward-headers-strategy=native}
 * with {@code server.tomcat.remoteip.internal-proxies} set to the addresses of the proxies).
 */
public class IpRateLimitFilter extends RateLimitFilter {

    public IpRateLimitFilter(ObjectMapper mapper, List<RateLimitRepository> repositories) {
        super(mapper, repositories);
    }

    @Override
    protected String extractKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
