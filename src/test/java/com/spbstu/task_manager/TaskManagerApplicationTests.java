package com.spbstu.task_manager;

import com.spbstu.task_manager.model.User; // импорт для тестов с репо
import com.spbstu.task_manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;

// Наследуемся от нашего базового класса с Testcontainers
class TaskManagerApplicationTests extends AbstractIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		// Простая проверка, что контекст приложения успешно загрузился
		assertThat(applicationContext).isNotNull();
		System.out.println("Test PostgreSQL JDBC URL: " + postgreSQLContainer.getJdbcUrl());
		System.out.println("Test PostgreSQL Username: " + postgreSQLContainer.getUsername());
		System.out.println("Test PostgreSQL Password: " + postgreSQLContainer.getPassword());
		// Можно добавить проверки, что ключевые бины (например, репозитории, сервисы) присутствуют в контексте
		assertThat(applicationContext.getBean(UserRepository.class)).isNotNull();
	}
}