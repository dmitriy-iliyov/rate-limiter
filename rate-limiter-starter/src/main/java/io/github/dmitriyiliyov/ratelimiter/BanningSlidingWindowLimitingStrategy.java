package io.github.dmitriyiliyov.ratelimiter;

import lombok.Builder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Sliding window with a ban: a client may make {@code maxAttemptCount} attempts within any {@code windowSize}.
 * The attempt over the limit bans the client for {@code lockTime} and clears its window; attempts made during the
 * ban are rejected and not counted.
 * <p>
 * Time is taken from the Redis server, so the clocks of application instances do not affect the window.
 * <p>
 * Runs the {@code banningSlidingWindowScript} bean of {@link ScriptAutoConfiguration}.
 * <p>
 * The Redis key of a client is {@code keyTemplate} formatted with the client key; the ban marker lives under the
 * same key prefixed with {@code blocked:}. The two keys may hash to different slots, so the strategy does not work
 * on Redis Cluster.
 * <p>
 * The {@code redisTemplate} has to serialize keys and values with
 * {@link org.springframework.data.redis.serializer.StringRedisSerializer}: the script arguments go through the value
 * serializer. Construction fails with {@link NullPointerException} on a missing argument and with
 * {@link IllegalArgumentException} on a {@code keyTemplate} without {@code %s}, a non-positive
 * {@code maxAttemptCount} or a duration shorter than 1 ms.
 */
@Builder
public class BanningSlidingWindowLimitingStrategy implements LimitingStrategy {

    private final RedisTemplate<String, Boolean> redisTemplate;
    private final DefaultRedisScript<Boolean> script;
    private final String keyTemplate;
    private final long maxAttemptCount;
    private final Duration windowSize;
    private final Duration lockTime;

    public BanningSlidingWindowLimitingStrategy(RedisTemplate<String, Boolean> redisTemplate,
                                                DefaultRedisScript<Boolean> script,
                                                String keyTemplate,
                                                long maxAttemptCount,
                                                Duration windowSize,
                                                Duration lockTime) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.script = Objects.requireNonNull(script, "script must not be null");
        this.keyTemplate = LimitingStrategyUtils.requireKeyTemplate(keyTemplate);
        this.maxAttemptCount = LimitingStrategyUtils.requirePositive(maxAttemptCount, "maxAttemptCount");
        this.windowSize = LimitingStrategyUtils.requirePositiveMillis(windowSize, "windowSize");
        this.lockTime = LimitingStrategyUtils.requirePositiveMillis(lockTime, "lockTime");
    }

    @Override
    public RateLimitStatus limit(String key) {
        Boolean result = redisTemplate.execute(
                script,
                List.of(keyTemplate.formatted(key), "blocked:" + keyTemplate.formatted(key)),
                UUID.randomUUID().toString(),
                String.valueOf(windowSize.toMillis()),
                String.valueOf(lockTime.toMillis()),
                String.valueOf(maxAttemptCount)
        );
        return RateLimitStatus.fromBoolean(result);
    }
}
