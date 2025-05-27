package com.spbstu.task_manager.service;

import com.spbstu.task_manager.event.TaskCreatedEvent;
import com.spbstu.task_manager.model.Notification;
import com.spbstu.task_manager.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // Слушатель Kafka для событий создания задач
    @KafkaListener(
            topics = "${app.kafka.topic.task-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional // Важно для атомарного сохранения уведомления
    public void handleTaskCreatedEvent(@Payload TaskCreatedEvent event) {
        log.info("Received TaskCreatedEvent from Kafka: {}", event);

        if (event == null || event.getUserId() == null || event.getTaskId() == null) {
            log.warn("Received invalid TaskCreatedEvent (null or missing IDs), skipping notification creation: {}", event);
            return;
        }

        try {
            String message = String.format("Вам назначена новая задача: '%s' (ID задачи: %d)",
                    event.getTaskTitle() != null ? event.getTaskTitle() : "Без названия",
                    event.getTaskId());

            // Используем новый/публичный метод createNotification
            this.createNotification(new Notification(message, event.getUserId()));
            // Логирование о создании уведомления теперь будет в методе createNotification
            // или можно оставить здесь специфичное для Kafka-события
            log.info("Notification for new task (from Kafka event) created for user {} regarding task {} with title '{}'",
                    event.getUserId(), event.getTaskId(), event.getTaskTitle());

        } catch (Exception e) {
            log.error("Error processing TaskCreatedEvent and creating notification for event: " + event, e);
        }
    }

    /**
     * Публичный метод для создания и сохранения уведомления.
     * Может вызываться из других сервисов (например, TaskSchedulerService).
     * @param notification объект Notification для сохранения
     * @return сохраненный объект Notification
     */
    @Transactional
    public Notification createNotification(Notification notification) {
        if (notification == null || notification.getUserId() == null || notification.getMessage() == null) {
            log.warn("Attempted to create an invalid notification (null or missing fields): {}", notification);
            // Можно выбросить исключение или вернуть null, в зависимости от требуемого поведения
            throw new IllegalArgumentException("Notification, its userId, and message cannot be null.");
        }
        // Поле timestamp в Notification будет установлено через @PrePersist перед сохранением
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification saved with ID: {}, Message: '{}', for User ID: {}",
                savedNotification.getId(), savedNotification.getMessage(), savedNotification.getUserId());
        return savedNotification;
    }

    // Методы для получения уведомлений остаются, так как NotificationController их использует
    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications(Long userId) {
        log.debug("Fetching all notifications for user {} from DB", userId);
        return notificationRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getPendingNotifications(Long userId) {
        log.debug("Fetching pending notifications for user {} from DB", userId);
        return notificationRepository.findByUserId(userId); // Логика "pending" не менялась
    }

    /**
     * Пример асинхронного метода для фоновой обработки.
     * Этот метод будет выполнен в отдельном потоке.
     * @param data Какие-то данные для обработки
     */
    @Async // Помечаем метод как асинхронный
    public void performBackgroundProcessing(String data) {
        log.info("Async: Starting background processing for data: {} on thread: {}", data, Thread.currentThread().getName());
        try {
            // Имитируем длительную операцию
            TimeUnit.SECONDS.sleep(5); // Ждем 5 секунд
            log.info("Async: Simulated long operation completed for data: {}", data);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания
            log.error("Async: Background processing interrupted for data: {}", data, e);
        }
        log.info("Async: Finished background processing for data: {} on thread: {}", data, Thread.currentThread().getName());
    }
}