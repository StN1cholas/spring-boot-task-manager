package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationServiceTest {

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService();
    }

    @Test
    void createNotification_shouldAddNotificationAndReturnItWithId() {
        // Arrange
        Notification newNotification = new Notification(null, "Test Message", 1L);

        // Act
        Notification createdNotification = notificationService.createNotification(newNotification);

        // Assert
        assertThat(createdNotification).isNotNull();
        assertThat(createdNotification.getId()).isNotNull().isEqualTo(1L);
        assertThat(createdNotification.getMessage()).isEqualTo("Test Message");
        assertThat(createdNotification.getUserId()).isEqualTo(1L);
        assertThat(createdNotification.getTimestamp()).isNotNull();

        // Дополнительная проверка (хотя getAllNotifications уже это проверяет)
        // Можно было бы добавить getNotificationById, если бы он был
        List<Notification> userNotifications = notificationService.getAllNotifications(1L);
        assertThat(userNotifications).contains(createdNotification);
    }

    @Test
    void getAllNotifications_shouldReturnAllNotificationsForSpecificUser() {
        // Arrange
        Long userId1 = 1L;
        Long userId2 = 2L;

        Notification n1u1 = notificationService.createNotification(new Notification(null, "N1 U1", userId1));
        Notification n2u1 = notificationService.createNotification(new Notification(null, "N2 U1", userId1));
        notificationService.createNotification(new Notification(null, "N1 U2", userId2)); // For other user

        // Act
        List<Notification> user1Notifications = notificationService.getAllNotifications(userId1);

        // Assert
        assertThat(user1Notifications)
                .isNotNull()
                .hasSize(2)
                .extracting(Notification::getMessage)
                .containsExactlyInAnyOrder("N1 U1", "N2 U1");
    }

    @Test
    void getAllNotifications_shouldReturnEmptyList_whenUserHasNoNotifications() {
        // Arrange
        Long userIdWithNoNotifications = 3L;
        notificationService.createNotification(new Notification(null, "N1 U1", 1L)); // For other user

        // Act
        List<Notification> notifications = notificationService.getAllNotifications(userIdWithNoNotifications);

        // Assert
        assertThat(notifications).isNotNull().isEmpty();
    }

    @Test
    void getPendingNotifications_shouldReturnAllNotificationsForUser_asPerCurrentLogic() {
        // Arrange
        Long userId1 = 1L;
        Long userId2 = 2L;

        // В текущей реализации getPendingNotifications возвращает то же, что и getAllNotifications
        Notification n1u1 = notificationService.createNotification(new Notification(null, "Pending N1 U1", userId1));
        Notification n2u1 = notificationService.createNotification(new Notification(null, "Pending N2 U1", userId1));
        notificationService.createNotification(new Notification(null, "Pending N1 U2", userId2));

        // Act
        List<Notification> pendingUser1Notifications = notificationService.getPendingNotifications(userId1);

        // Assert
        assertThat(pendingUser1Notifications)
                .isNotNull()
                .hasSize(2)
                .extracting(Notification::getMessage)
                .containsExactlyInAnyOrder("Pending N1 U1", "Pending N2 U1");
    }

    @Test
    void getPendingNotifications_shouldReturnEmptyList_whenUserHasNoNotifications() {
        // Arrange
        Long userIdWithNoNotifications = 3L;

        // Act
        List<Notification> notifications = notificationService.getPendingNotifications(userIdWithNoNotifications);

        // Assert
        assertThat(notifications).isNotNull().isEmpty();
    }
}