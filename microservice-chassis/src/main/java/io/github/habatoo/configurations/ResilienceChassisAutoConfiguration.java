package io.github.habatoo.configurations;

import io.github.habatoo.properties.ResilienceProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@AutoConfiguration
@EnableConfigurationProperties(ResilienceProperties.class)
public class ResilienceChassisAutoConfiguration {

    @Bean
    public CircuitBreakerConfigCustomizer defaultCustomizer(ResilienceProperties props) {
        return CircuitBreakerConfigCustomizer.of(props.instanceName(), builder -> builder
                .slidingWindowSize(props.slidingWindowSize().intValue())
                .failureRateThreshold(props.failureRateThreshold().floatValue())
                .waitDurationInOpenState(Duration.ofSeconds(props.waitDurationInOpenState()))
                .permittedNumberOfCallsInHalfOpenState(props.permittedNumberOfCallsInHalfOpenState().intValue())
        );
    }
}
