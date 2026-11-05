package io.github.dmitriyiliyov.ratelimiter;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * One Redis container shared by every strategy integration test of the run, flushed before each test.
 */
abstract class RedisContainerSupport {

    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static final RedisTemplate<String, Boolean> REDIS_TEMPLATE;

    static {
        REDIS.start();
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379))
        );
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();

        REDIS_TEMPLATE = new RedisTemplate<>();
        REDIS_TEMPLATE.setConnectionFactory(connectionFactory);
        REDIS_TEMPLATE.setKeySerializer(new StringRedisSerializer());
        REDIS_TEMPLATE.setValueSerializer(new StringRedisSerializer());
        REDIS_TEMPLATE.afterPropertiesSet();
    }

    @BeforeEach
    void flushRedis() {
        REDIS_TEMPLATE.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    static DefaultRedisScript<Boolean> script(String fileName) {
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(fileName)));
        script.setResultType(Boolean.class);
        return script;
    }
}
