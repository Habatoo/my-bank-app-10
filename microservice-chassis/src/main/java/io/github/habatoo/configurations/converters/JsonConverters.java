package io.github.habatoo.configurations.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

import java.util.Map;

public class JsonConverters {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    @WritingConverter
    public static class MapToJsonConverter implements Converter<Map<String, Object>, Json> {
        @Override
        @NonNull
        public Json convert(@NonNull Map<String, Object> source) {
            try {
                return Json.of(objectMapper.writeValueAsString(source));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error converting Map to JSON", e);
            }
        }
    }

    @ReadingConverter
    public static class JsonToMapConverter implements Converter<Json, Map<String, Object>> {
        @Override
        @NonNull
        public Map<String, Object> convert(@NonNull Json source) {
            try {
                return objectMapper.readValue(source.asString(), new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception e) {
                throw new RuntimeException("Error converting JSON to Map", e);
            }
        }
    }
}
