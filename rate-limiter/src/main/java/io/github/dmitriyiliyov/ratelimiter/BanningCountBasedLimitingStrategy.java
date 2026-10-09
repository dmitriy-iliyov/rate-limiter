package io.github.dmitriyiliyov.ratelimiter;

import lombok.Builder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Builder
public class BanningCountBasedLimitingStrategy implements LimitingStrategy {

    private final RedisTemplate<String, Boolean> redisTemplate;
    private final DefaultRedisScript<Boolean> script;
    private final String keyTemplate;
    private final long maxAttemptCount;
    private final Duration observeTime;
    private final Duration lockTime;

    public BanningCountBasedLimitingStrategy(RedisTemplate<String, Boolean> redisTemplate,
                                             DefaultRedisScript<Boolean> script,
                                             String keyTemplate,
                                             long maxAttemptCount,
                                             Duration observeTime,
                                             Duration lockTime) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.script = Objects.requireNonNull(script, "script must not be null");
        this.keyTemplate = LimitingStrategyUtils.requireKeyTemplate(keyTemplate);
        this.maxAttemptCount = LimitingStrategyUtils.requirePositive(maxAttemptCount, "maxAttemptCount");
        this.observeTime = LimitingStrategyUtils.requirePositiveMillis(observeTime, "observeTime");
        this.lockTime = LimitingStrategyUtils.requirePositiveMillis(lockTime, "lockTime");
    }

    @Override
    public RateLimitStatus limit(String key) {
        Boolean result = redisTemplate.execute(
                script,
                List.of(keyTemplate.formatted(key), "blocked:" + keyTemplate.formatted(key)),
                String.valueOf(observeTime.toMillis()),
                String.valueOf(lockTime.toMillis()),
                String.valueOf(maxAttemptCount)
        );
        return RateLimitStatus.fromBoolean(result);
    }
}
