package io.github.habatoo.base;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Базовый класс для интеграционных тестов с использованием Testcontainers.
 * <p>
 * Обеспечивает автоматический запуск контейнера PostgreSQL, настройку реактивного подключения R2DBC,
 * выполнение миграций Liquibase и инициализацию окружения (заглушка JWT).
 * Содержит вспомогательные методы для создания тестовых сущностей пользователей и счетов.
 */
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class BaseTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_pass")
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(), postgres.getMappedPort(5432), postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        registry.add("spring.liquibase.url", postgres::getJdbcUrl);
        registry.add("spring.liquibase.user", postgres::getUsername);
        registry.add("spring.liquibase.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");

        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost");
    }
}
