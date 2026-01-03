package io.github.habatoo.configurations;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class JsonChassisAutoConfiguration {

    @Value("${spring.application.date:yyyy-MM-dd HH:mm:ss}")
    private String applicationDateFormat;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            builder.simpleDateFormat(applicationDateFormat);
            builder.modules(new JavaTimeModule());

            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            builder.featuresToEnable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        };
    }
}
