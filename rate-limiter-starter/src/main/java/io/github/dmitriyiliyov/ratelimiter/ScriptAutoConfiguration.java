package io.github.dmitriyiliyov.ratelimiter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

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
