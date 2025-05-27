package com.spbstu.task_manager.service;

import com.spbstu.task_manager.event.TaskCreatedEvent;
import com.spbstu.task_manager.model.Notification;
import com.spbstu.task_manager.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        // @Payload указывает, что параметр event должен быть заполнен из тела сообщения Kafka.
        // Spring Kafka (с JsonDeserializer) попытается преобразовать JSON в TaskCreatedEvent.
        log.info("Received TaskCreatedEvent from Kafka: {}", event);

        if (event == null || event.getUserId() == null || event.getTaskId() == null) {
            log.warn("Received invalid TaskCreatedEvent (null or missing IDs), skipping notification creation: {}", event);
            return;
        }

        try {
            String message = String.format("Вам назначена новая задача: '%s' (ID задачи: %d)",
                    event.getTaskTitle() != null ? event.getTaskTitle() : "Без названия",
                    event.getTaskId());

            Notification notification = new Notification(message, event.getUserId());
            // Поле timestamp в Notification будет установлено через @PrePersist

            notificationRepository.save(notification);
            log.info("Notification created for user {} regarding task {} with title '{}'",
                    event.getUserId(), event.getTaskId(), event.getTaskTitle());

        } catch (Exception e) {
            log.error("Error processing TaskCreatedEvent and creating notification for event: " + event, e);
        }
    }

    // Методы для получения уведомлений остаются, так как NotificationController их использует
    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications(Long userId) {
        log.debug("Fetching all notifications for user {} from DB", userId); // Изменено на debug для меньшего шума
        return notificationRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getPendingNotifications(Long userId) {
        log.debug("Fetching pending notifications for user {} from DB", userId); // Изменено на debug
        return notificationRepository.findByUserId(userId); // Логика "pending" не менялась
    }
}