package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.Notification;
import com.spbstu.task_manager.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification1, notification2;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        notification1 = new Notification(1L, "Message 1", LocalDateTime.now().minusHours(1), userId);
        notification2 = new Notification(2L, "Message 2", LocalDateTime.now(), userId);
    }

    @Test
    void getAllNotifications_shouldReturnNotificationsForUser() {
        // Arrange
        given(notificationRepository.findByUserId(userId)).willReturn(Arrays.asList(notification1, notification2));

        // Act
        List<Notification> notifications = notificationService.getAllNotifications(userId);

        // Assert
        assertThat(notifications).isNotNull().hasSize(2);
        assertThat(notifications).containsExactlyInAnyOrder(notification1, notification2);
        verify(notificationRepository).findByUserId(userId);
    }

    @Test
    void getPendingNotifications_shouldReturnAllNotificationsForUser_asPerCurrentLogic() {
        // Arrange
        // В текущей реализации getPendingNotifications вызывает findByUserId
        given(notificationRepository.findByUserId(userId)).willReturn(Arrays.asList(notification1, notification2));

        // Act
        List<Notification> pendingNotifications = notificationService.getPendingNotifications(userId);

        // Assert
        assertThat(pendingNotifications).isNotNull().hasSize(2);
        verify(notificationRepository).findByUserId(userId);
    }

    @Test
    void createNotification_shouldSaveAndReturnNotification() {
        // Arrange
        Notification newNotification = new Notification("New Message", userId);
        // @PrePersist в модели Notification установит timestamp
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            Notification savedNotification = new Notification(3L, n.getMessage(), LocalDateTime.now(), n.getUserId());
            if (n.getTimestamp() == null) savedNotification.setTimestamp(LocalDateTime.now()); // Имитация @PrePersist
            return savedNotification;
        });

        // Act
        Notification createdNotification = notificationService.createNotification(newNotification);

        // Assert
        assertThat(createdNotification).isNotNull();
        assertThat(createdNotification.getId()).isEqualTo(3L); // Пример ID
        assertThat(createdNotification.getMessage()).isEqualTo("New Message");
        assertThat(createdNotification.getTimestamp()).isNotNull();
        verify(notificationRepository).save(any(Notification.class));
    }
}