package com.spbstu.task_manager;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:15-alpine") // Можно выбрать другую актуальную версию PostgreSQL
                    .withDatabaseName("test-taskmanagerdb")
                    .withUsername("testuser")
                    .withPassword("testpass");

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Убедимся, что Flyway включен и настроен для работы с БД в контейнере
        registry.add("spring.flyway.enabled", () -> "true");
        // ddl-auto должно быть 'validate' или 'none' в основном application.properties
        // Если нужно переопределить для тестов:
        // registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}