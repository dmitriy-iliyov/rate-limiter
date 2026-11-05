package io.github.dmitriyiliyov.ratelimiter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * Registers the Lua scripts of the strategies as {@link DefaultRedisScript} beans: {@code banningCountBasedScript},
 * {@code banningSlidingWindowScript} and {@code slidingWindowScript}. The beans share one type, so they are
 * injected by name, and a bean of the application with one of these names replaces only that script.
 * <p>
 * Strategies, repositories and the filter are not auto-configured: each endpoint needs its own policy, so the
 * application declares them as its own beans.
 */
@AutoConfiguration
public class ScriptAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "banningCountBasedScript")
    public DefaultRedisScript<Boolean> banningCountBasedScript() {
        return createScriptOf("banning_count_based.lua");
    }

    @Bean
    @ConditionalOnMissingBean(name = "banningSlidingWindowScript")
    public DefaultRedisScript<Boolean> banningSlidingWindowScript() {
        return createScriptOf("banning_sliding_window.lua");
    }

    @Bean
    @ConditionalOnMissingBean(name = "slidingWindowScript")
    public DefaultRedisScript<Boolean> slidingWindowScript() {
        return createScriptOf("sliding_window.lua");
    }

    private static DefaultRedisScript<Boolean> createScriptOf(String fileName) {
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(fileName)));
        script.setResultType(Boolean.class);
        return script;
    }
}
