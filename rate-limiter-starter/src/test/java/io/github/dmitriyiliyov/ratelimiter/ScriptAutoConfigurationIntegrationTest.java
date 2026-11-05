package io.github.dmitriyiliyov.ratelimiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptAutoConfigurationIntegrationTest {

    final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ScriptAutoConfiguration.class));

    @Test
    @DisplayName("IT context should register one script bean per strategy loaded from classpath")
    void context_shouldRegisterOneScriptBeanPerStrategyLoadedFromClasspath() {
        contextRunner.run(context -> {
            assertThat(context).getBeans(DefaultRedisScript.class).hasSize(3);
            assertScript(context.getBean("banningCountBasedScript", DefaultRedisScript.class), "INCR");
            assertScript(context.getBean("banningSlidingWindowScript", DefaultRedisScript.class), "blocked_key");
            assertScript(context.getBean("slidingWindowScript", DefaultRedisScript.class), "ZADD");
        });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"banningCountBasedScript", "banningSlidingWindowScript", "slidingWindowScript"})
    @DisplayName("IT context when user declares a script bean with the same name should replace only that script")
    void context_whenUserDeclaresScriptBeanWithSameName_shouldReplaceOnlyThatScript(String beanName) {
        // given
        DefaultRedisScript<Boolean> userScript = new DefaultRedisScript<>("return 1", Boolean.class);

        // when & then
        contextRunner
                .withBean(beanName, DefaultRedisScript.class, () -> userScript)
                .run(context -> {
                    assertThat(context).getBeans(DefaultRedisScript.class).hasSize(3);
                    assertThat(context.getBean(beanName)).isSameAs(userScript);
                    assertThat(context).hasBean("banningCountBasedScript")
                            .hasBean("banningSlidingWindowScript")
                            .hasBean("slidingWindowScript");
                });
    }

    @Test
    @DisplayName("IT AutoConfiguration.imports should register ScriptAutoConfig for Spring Boot to discover")
    void imports_shouldRegisterScriptAutoConfigForSpringBootToDiscover() {
        // when
        ImportCandidates candidates = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader());

        // then
        assertThat(candidates.getCandidates()).contains(ScriptAutoConfiguration.class.getName());
    }

    private static void assertScript(DefaultRedisScript<?> script, String expectedFragment) {
        assertThat(script.getResultType()).isEqualTo(Boolean.class);
        assertThat(script.getScriptAsString()).contains(expectedFragment);
    }
}
