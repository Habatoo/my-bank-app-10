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

/**
 * Набор конвертеров для преобразования данных между форматами Java (Map) и JSON (R2DBC).
 * <p>
 * Используется в реактивных репозиториях (Spring Data R2DBC) для автоматического маппинга
 * полей типа JSON/JSONB из PostgreSQL в структуры данных Java.
 * </p>
 * <p>
 * Конфигурация включает {@link JavaTimeModule} для корректной обработки типов даты и времени
 * и отключает сериализацию дат в числовые метки (timestamps).
 * </p>
 */
public class JsonConverters {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    /**
     * Конвертер для записи данных в базу данных (Map -> JSON).
     * <p>
     * Преобразует {@link Map} в объект {@link Json}, который может быть сохранен
     * в колонку типа JSONB в PostgreSQL.
     * </p>
     */
    @WritingConverter
    public static class MapToJsonConverter implements Converter<Map<String, Object>, Json> {
        /**
         * Преобразует карту объектов в JSON-строку.
         *
         * @param source исходная карта данных.
         * @return объект {@link Json} для сохранения в БД.
         * @throws RuntimeException если возникла ошибка при сериализации в JSON.
         */
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

    /**
     * Конвертер для чтения данных из базы данных (JSON -> Map).
     * <p>
     * Извлекает данные из объекта {@link Json} (тип JSONB в PostgreSQL)
     * и преобразует их обратно в {@link Map}.
     * </p>
     */
    @ReadingConverter
    public static class JsonToMapConverter implements Converter<Json, Map<String, Object>> {
        /**
         * Десериализует JSON-строку в карту объектов Java.
         *
         * @param source JSON-объект из базы данных.
         * @return карта данных {@link Map}.
         * @throws RuntimeException если возникла ошибка при десериализации JSON.
         */
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
