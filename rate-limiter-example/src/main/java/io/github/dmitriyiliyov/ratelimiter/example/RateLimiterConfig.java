package io.github.dmitriyiliyov.ratelimiter.example;

import io.github.dmitriyiliyov.ratelimiter.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Configuration
public class RateLimiterConfig {

    @Bean
    public RedisTemplate<String, Boolean> rateLimitRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Boolean> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public RateLimitRepository guardedRateLimitRepository(RedisTemplate<String, Boolean> rateLimitRedisTemplate,
                                                          @Qualifier("banningCountBasedScript")
                                                          DefaultRedisScript<Boolean> script) {
        BanningCountBasedLimitingStrategy strategy = BanningCountBasedLimitingStrategy.builder()
                .redisTemplate(rateLimitRedisTemplate)
                .script(script)
                .keyTemplate("rate-limit:guarded:%s")
                .maxAttemptCount(3)
                .observeTime(Duration.ofSeconds(10))
                .lockTime(Duration.ofMinutes(15))
                .build();
        return DefaultRateLimitRepository.builder()
                .strategy(strategy)
                .targetUrl("/api/test-rate-limiter/guarded")
                .build();
    }

    @Bean
    public RateLimitFilter rateLimitFilter(ObjectMapper mapper, List<RateLimitRepository> repositories) {
        return new IpRateLimitFilter(mapper, repositories);
    }

    // the filter runs inside the security chain, so the servlet container must not register it a second time
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
