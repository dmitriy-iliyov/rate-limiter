package io.github.dmitriyiliyov.ratelimiter;

import java.util.Objects;

/**
 * The decision of a {@link LimitingStrategy} about one attempt.
 */
public enum RateLimitStatus {
    ALLOWED, BLOCKED;

    /**
     * Converts the result of a limiting script, where {@code true} means the attempt is allowed.
     *
     * @param b the script result
     * @return {@link #ALLOWED} for {@code true}, {@link #BLOCKED} for {@code false}
     * @throws NullPointerException if {@code b} is {@code null}
     */
    public static RateLimitStatus fromBoolean(Boolean b) {
        Objects.requireNonNull(b, "boolean value cannot be null");
        if (b.equals(Boolean.TRUE)) {
            return RateLimitStatus.ALLOWED;
        } else {
            return RateLimitStatus.BLOCKED;
        }
    }
}
