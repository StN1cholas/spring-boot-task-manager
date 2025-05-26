package com.spbstu.task_manager.repository;

import com.spbstu.task_manager.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    // Если бы была логика для "pending" уведомлений на уровне БД (например, поле isRead=false),
    // можно было бы добавить метод:
    // List<Notification> findByUserIdAndIsReadFalse(Long userId);
    // Пока, по вашей логике, getPendingNotifications возвращает все уведомления пользователя.
}