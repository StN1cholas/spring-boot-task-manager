package com.spbstu.task_manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.spbstu.task_manager.model.Notification;
import com.spbstu.task_manager.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@WebMvcTest(NotificationController.class)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationService notificationServiceMock;

    @TestConfiguration
    static class NotificationControllerTestConfiguration {
        @Bean
        public NotificationService notificationService() {
            return Mockito.mock(NotificationService.class);
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void getAllNotifications_shouldReturnListOfNotificationsForUser() throws Exception {
        // Arrange
        Long userId = 1L;
        // Создаем объекты Notification так, как они были бы получены из сервиса (с ID и timestamp)
        Notification n1 = new Notification(1L, "Notification 1", LocalDateTime.now().minusHours(1), userId);
        Notification n2 = new Notification(2L, "Notification 2", LocalDateTime.now(), userId);
        List<Notification> notifications = Arrays.asList(n1, n2);

        given(notificationServiceMock.getAllNotifications(userId)).willReturn(notifications);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1))) // is() из Hamcrest
                .andExpect(jsonPath("$[0].message", is("Notification 1")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].message", is("Notification 2")));
    }

    @Test
    void getAllNotifications_shouldReturnEmptyList_whenUserHasNoNotifications() throws Exception {
        // Arrange
        Long userId = 1L;
        given(notificationServiceMock.getAllNotifications(userId)).willReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/notifications/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getPendingNotifications_shouldReturnListOfPendingNotificationsForUser() throws Exception {
        // Arrange
        Long userId = 1L;
        Notification n1 = new Notification(1L, "Pending Notification 1", LocalDateTime.now(), userId);
        List<Notification> pendingNotifications = Collections.singletonList(n1);

        given(notificationServiceMock.getPendingNotifications(userId)).willReturn(pendingNotifications);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/pending/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].message", is("Pending Notification 1")));
    }
}