package io.github.dmitriyiliyov.ratelimiter;

/**
 * Decides whether a client may make one more attempt and records the attempt in the same step.
 * <p>
 * A call is not a query: every call counts, so it is made exactly once per request.
 */
public interface LimitingStrategy {

    /**
     * Records an attempt of the client and decides whether it is allowed.
     *
     * @param key the client key, such as an IP address or a user id; clients with equal keys share one limit
     * @return {@link RateLimitStatus#ALLOWED} if the attempt is within the limit, {@link RateLimitStatus#BLOCKED}
     *         otherwise; never {@code null}
     */
    RateLimitStatus limit(String key);
}
