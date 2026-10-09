package io.github.dmitriyiliyov.ratelimiter;

import java.time.Duration;
import java.util.Objects;

final class LimitingStrategyUtils {

    private static final String KEY_PLACEHOLDER = "%s";

    private LimitingStrategyUtils() { }

    static String requireKeyTemplate(String keyTemplate) {
        Objects.requireNonNull(keyTemplate, "keyTemplate must not be null");
        if (!keyTemplate.contains(KEY_PLACEHOLDER)) {
            throw new IllegalArgumentException(
                    "keyTemplate must contain '%s' placeholder for the key, got: %s".formatted(KEY_PLACEHOLDER, keyTemplate)
            );
        }
        return keyTemplate;
    }

    static long requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException("%s must be positive, got: %d".formatted(name, value));
        }
        return value;
    }

    static Duration requirePositiveMillis(Duration value, String name) {
        Objects.requireNonNull(value, "%s must not be null".formatted(name));
        if (value.toMillis() <= 0) {
            throw new IllegalArgumentException("%s must be at least 1 ms, got: %s".formatted(name, value));
        }
        return value;
    }
}
