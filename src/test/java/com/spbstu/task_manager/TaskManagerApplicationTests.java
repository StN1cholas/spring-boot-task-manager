package com.spbstu.task_manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.test.context.EmbeddedKafka; // <<< ИМПОРТ
import org.springframework.test.annotation.DirtiesContext; // Для очистки контекста, если нужно
import org.springframework.boot.test.mock.mockito.MockBean; // <<< ИМПОРТ
import org.springframework.kafka.core.KafkaTemplate;      // <<< ИМПОРТ

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(    // <<< АННОТАЦИЯ ДЛЯ ВСТРОЕННОГО KAFKA
		partitions = 1,
		brokerProperties = { "listeners=PLAINTEXT://localhost:9093", "port=9093" }, // Случайный порт для тестов
		topics = { "${app.kafka.topic.task-created}" } // Указываем топик, который должен быть создан
)
@DirtiesContext // Пересоздает контекст для каждого теста/класса, если нужно чистое состояние Kafka
// class TaskManagerApplicationTests extends AbstractIntegrationTest { // AbstractIntegrationTest здесь может конфликтовать, т.к. он настраивает PG
class TaskManagerApplicationTests { // Если этот тест только для Kafka, можно не наследоваться от AbstractIntegrationTest

	@Autowired
	private ApplicationContext applicationContext;

	@MockBean // <<< МОКИРУЕМ KafkaTemplate
	private KafkaTemplate<String, ?> kafkaTemplate;

	@Test
	void contextLoads() {
		assertThat(applicationContext).isNotNull();
		// Проверяем, что KafkaTemplate и KafkaListenerContainerFactory созданы
		assertThat(applicationContext.containsBean("kafkaTemplate")).isTrue();
		assertThat(applicationContext.containsBean("kafkaListenerContainerFactory")).isTrue();
		System.out.println("ApplicationContext loaded successfully with EmbeddedKafka.");
	}
}