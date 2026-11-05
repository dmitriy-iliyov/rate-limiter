package io.github.dmitriyiliyov.ratelimiter;

/**
 * The rate limit of one endpoint, picked by {@link RateLimitFilter} for the requests to {@link #getTargetUrl()}.
 * <p>
 * Every endpoint has at most one repository: a second one with the same target URL fails the construction of
 * the filter.
 */
public interface RateLimitRepository {

    /**
     * @return the decoded request URI of the guarded endpoint, including the context path, such as
     *         {@code /api/login}
     */
    String getTargetUrl();

    /**
     * Records an attempt of the client on the endpoint and decides whether it is allowed.
     *
     * @param key the client key extracted by the filter
     * @return the decision about the attempt; never {@code null}
     */
    RateLimitStatus increment(String key);
}
