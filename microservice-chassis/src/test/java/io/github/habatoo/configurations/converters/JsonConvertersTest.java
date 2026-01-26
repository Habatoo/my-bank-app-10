package io.github.habatoo.configurations.converters;

import io.r2dbc.postgresql.codec.Json;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тесты конвертеров для преобразования данных между форматами Java (Map) и JSON (R2DB).
 */
@DisplayName("Юнит-тесты для JsonConverters")
class JsonConvertersTest {

    private final JsonConverters.MapToJsonConverter writingConverter = new JsonConverters.MapToJsonConverter();
    private final JsonConverters.JsonToMapConverter readingConverter = new JsonConverters.JsonToMapConverter();

    @Nested
    @DisplayName("Тесты MapToJsonConverter (Запись в БД)")
    class MapToJsonTests {

        @Test
        @DisplayName("Должен успешно конвертировать Map в объект Json")
        void shouldConvertMapToJson() {
            Map<String, Object> source = Map.of(
                    "id", 123,
                    "name", "test-event",
                    "date", LocalDate.of(2026, 1, 22)
            );

            Json result = writingConverter.convert(source);

            assertThat(result).isNotNull();
            String jsonString = result.asString();

            assertThat(jsonString)
                    .contains("\"id\":123")
                    .contains("\"name\":\"test-event\"")
                    .contains("\"date\":\"2026-01-22\"");
        }
    }

    @Nested
    @DisplayName("Тесты JsonToMapConverter (Чтение из БД)")
    class JsonToMapTests {

        @Test
        @DisplayName("Должен успешно конвертировать объект Json в Map")
        void shouldConvertJsonToMap() {
            String jsonRaw = "{\"username\":\"habatoo\",\"roles\":[\"ADMIN\",\"USER\"]}";
            Json source = Json.of(jsonRaw);

            Map<String, Object> result = readingConverter.convert(source);

            assertThat(result)
                    .isNotNull()
                    .containsEntry("username", "habatoo")
                    .containsKey("roles");

            assertThat(result.get("roles")).asList().containsExactly("ADMIN", "USER");
        }

        @Test
        @DisplayName("Должен выбрасывать RuntimeException при некорректном JSON")
        void shouldThrowExceptionOnInvalidJson() {
            Json invalidSource = Json.of("{invalid_json}");

            assertThatThrownBy(() -> readingConverter.convert(invalidSource))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error converting JSON to Map");
        }
    }
}
