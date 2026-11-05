package io.github.dmitriyiliyov.ratelimiter;

import lombok.Builder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
