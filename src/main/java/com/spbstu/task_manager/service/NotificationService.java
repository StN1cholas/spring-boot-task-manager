package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.Notification;
import com.spbstu.task_manager.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getPendingNotifications(Long userId) {
        // Текущая логика: "pending" это все уведомления пользователя.
        // Если бы была другая логика (например, isRead=false),
        // то здесь был бы другой метод репозитория.
        return notificationRepository.findByUserId(userId);
    }

    @Transactional
    public Notification createNotification(Notification notification) {
        // Установка timestamp здесь больше не нужна, если используется @PrePersist в модели
        // if (notification.getTimestamp() == null) {
        //     notification.setTimestamp(LocalDateTime.now());
        // }
        return notificationRepository.save(notification);
    }
}