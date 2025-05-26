package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.Notification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final Map<Long, Notification> notifications = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(0);

    public List<Notification> getAllNotifications(Long userId) {
        return notifications.values().stream()
                .filter(notification -> notification.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Notification> getPendingNotifications(Long userId) {
        // Здесь можно добавить логику для определения "pending" уведомлений,
        // например, по дате создания или статусу
        return notifications.values().stream()
                .filter(notification -> notification.getUserId().equals(userId)) // Просто возвращаем все уведомления для примера
                .collect(Collectors.toList());
    }

    public Notification createNotification(Notification notification) {
        Long id = idCounter.incrementAndGet();
        notification.setId(id);
        notifications.put(id, notification);
        return notification;
    }
}