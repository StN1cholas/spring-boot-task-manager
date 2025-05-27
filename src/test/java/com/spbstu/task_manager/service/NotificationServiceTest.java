package com.spbstu.task_manager.service;

import com.spbstu.task_manager.event.TaskCreatedEvent; // <<< ИМПОРТ
import com.spbstu.task_manager.model.Notification;
import com.spbstu.task_manager.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor; // <<< ИМПОРТ
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
// import java.util.Collections; // Больше не используется в этом примере
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyLong; // Больше не используется в этом примере
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never; // <<< ИМПОРТ
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification1, notification2;
    private Long userId = 1L;
    private TaskCreatedEvent validEvent;

    @BeforeEach
    void setUp() {
        // Эти объекты используются для тестов get*Notifications
        notification1 = new Notification(1L, "Message 1", LocalDateTime.now().minusHours(1), userId);
        notification2 = new Notification(2L, "Message 2", LocalDateTime.now(), userId);

        // Объект события для тестов processAndCreateNotificationFromEvent
        validEvent = new TaskCreatedEvent(100L, userId, "Новая важная задача");
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
        given(notificationRepository.findByUserId(userId)).willReturn(Arrays.asList(notification1, notification2));

        // Act
        List<Notification> pendingNotifications = notificationService.getPendingNotifications(userId);

        // Assert
        assertThat(pendingNotifications).isNotNull().hasSize(2);
        verify(notificationRepository).findByUserId(userId);
    }

    // --- Новые тесты для processAndCreateNotificationFromEvent ---

    @Test
    void processAndCreateNotificationFromEvent_shouldSaveNotification_whenEventIsValid() {
        // Arrange
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        // Настраиваем мок save, чтобы он возвращал переданный ему объект (для имитации сохранения)
        // или можно не настраивать, если проверяем только вызов и аргументы.
        // given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

        // Act
        notificationService.processAndCreateNotificationFromEvent(validEvent);

        // Assert
        verify(notificationRepository).save(notificationCaptor.capture()); // Проверяем, что save был вызван
        Notification capturedNotification = notificationCaptor.getValue(); // Получаем захваченный Notification

        assertThat(capturedNotification).isNotNull();
        assertThat(capturedNotification.getUserId()).isEqualTo(validEvent.getUserId());
        assertThat(capturedNotification.getMessage())
                .isEqualTo(String.format("Вам назначена новая задача: '%s' (ID задачи: %d)",
                        validEvent.getTaskTitle(), validEvent.getTaskId()));
        // Timestamp устанавливается через @PrePersist в модели, поэтому мы не можем точно предсказать его,
        // но можем проверить, что он не null, если это важно.
        // Для юнит-теста мы не проверяем работу @PrePersist, а только логику нашего метода.
    }

    @Test
    void processAndCreateNotificationFromEvent_shouldUseDefaultTitle_whenEventTitleIsNull() {
        // Arrange
        TaskCreatedEvent eventWithNullTitle = new TaskCreatedEvent(101L, userId, null);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        // Act
        notificationService.processAndCreateNotificationFromEvent(eventWithNullTitle);

        // Assert
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getMessage())
                .isEqualTo(String.format("Вам назначена новая задача: '%s' (ID задачи: %d)",
                        "Без названия", eventWithNullTitle.getTaskId()));
    }

    @Test
    void processAndCreateNotificationFromEvent_shouldNotSaveNotification_whenEventIsNull() {
        // Act
        notificationService.processAndCreateNotificationFromEvent(null);

        // Assert
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void processAndCreateNotificationFromEvent_shouldNotSaveNotification_whenUserIdIsNullInEvent() {
        // Arrange
        TaskCreatedEvent eventWithNullUserId = new TaskCreatedEvent(102L, null, "Задача без UserID");

        // Act
        notificationService.processAndCreateNotificationFromEvent(eventWithNullUserId);

        // Assert
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void processAndCreateNotificationFromEvent_shouldNotSaveNotification_whenTaskIdIsNullInEvent() {
        // Arrange
        TaskCreatedEvent eventWithNullTaskId = new TaskCreatedEvent(null, userId, "Задача без TaskID");

        // Act
        notificationService.processAndCreateNotificationFromEvent(eventWithNullTaskId);

        // Assert
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}