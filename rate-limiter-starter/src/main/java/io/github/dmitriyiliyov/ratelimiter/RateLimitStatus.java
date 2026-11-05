package io.github.dmitriyiliyov.ratelimiter;

import java.util.Objects;

public enum RateLimitStatus {
    ALLOWED, BLOCKED;

    public static RateLimitStatus fromBoolean(Boolean b) {
        Objects.requireNonNull(b, "boolean value cannot be null");
        if (b.equals(Boolean.TRUE)) {
            return RateLimitStatus.ALLOWED;
        } else {
            return RateLimitStatus.BLOCKED;
        }
    }
}
