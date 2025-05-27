package com.spbstu.task_manager.controller;
import org.springframework.kafka.core.KafkaTemplate;
import com.spbstu.task_manager.event.TaskCreatedEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.spbstu.task_manager.model.Task;
import com.spbstu.task_manager.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@WebMvcTest(TaskController.class)
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskService taskServiceMock;

    @TestConfiguration
    static class TaskControllerTestConfiguration {
        @Bean
        public TaskService taskService() {
            return Mockito.mock(TaskService.class);
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void getAllTasks_shouldReturnListOfTasksForUser() throws Exception {
        // Arrange
        Long userId = 1L;
        // Создаем объекты Task так, как они были бы получены из сервиса
        Task task1 = new Task(1L, "Task 1", "Desc 1", LocalDate.now().plusDays(1), LocalDateTime.now().minusDays(1), false, userId);
        Task task2 = new Task(2L, "Task 2", "Desc 2", LocalDate.now().plusDays(2), LocalDateTime.now().minusHours(5), false, userId);
        List<Task> tasks = Arrays.asList(task1, task2);

        given(taskServiceMock.getAllTasks(userId)).willReturn(tasks);

        // Act & Assert
        mockMvc.perform(get("/api/tasks/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }

    @Test
    void getAllTasks_shouldReturnEmptyList_whenUserHasNoTasks() throws Exception {
        Long userId = 1L;
        given(taskServiceMock.getAllTasks(userId)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tasks/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getPendingTasks_shouldReturnListOfPendingTasksForUser() throws Exception {
        Long userId = 1L;
        Task task1 = new Task(1L, "Pending Task 1", "Desc 1", LocalDate.now().plusDays(1), LocalDateTime.now(), false, userId);
        List<Task> pendingTasks = Collections.singletonList(task1);

        given(taskServiceMock.getPendingTasks(userId)).willReturn(pendingTasks);
        mockMvc.perform(get("/api/tasks/pending/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Pending Task 1")));
    }

    @Test
    void createTask_shouldReturnCreatedTaskAndHttpStatusCreated() throws Exception {
        // Arrange
        // Этот объект мы отправляем в запросе (без ID, creationDate)
        Task taskToCreateRequest = new Task("New Task", "New Desc", LocalDate.now().plusDays(3), 1L);

        // Этот объект имитирует то, что вернет сервис после сохранения в БД (с ID и creationDate)
        Task createdTaskFromService = new Task(1L, "New Task", "New Desc", LocalDate.now().plusDays(3), LocalDateTime.now(), false, 1L);

        given(taskServiceMock.createTask(any(Task.class))).willReturn(createdTaskFromService);

        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskToCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("New Task")))
                .andExpect(jsonPath("$.targetDate").value(LocalDate.now().plusDays(3).toString()));
    }

    @Test
    void deleteTask_shouldReturnHttpStatusNoContent() throws Exception {
        Long taskId = 1L;
        given(taskServiceMock.deleteTask(taskId)).willReturn(Optional.of(1L));

        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isNoContent());

        verify(taskServiceMock).deleteTask(taskId);
    }

    @Test
    void getTaskById_shouldReturnTask_whenExists() throws Exception {
        Long taskId = 1L;
        Long userId = 1L;
        Task task = new Task(taskId, "Found Task", "Desc", LocalDate.now().plusDays(1), LocalDateTime.now(), false, userId);

        given(taskServiceMock.getTaskById(taskId)).willReturn(task);

        mockMvc.perform(get("/api/tasks/id/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(taskId.intValue())))
                .andExpect(jsonPath("$.title", is("Found Task")));
    }

    @Test
    void getTaskById_shouldReturnHttpStatusNotFound_whenNotExists() throws Exception {
        Long taskId = 99L;
        given(taskServiceMock.getTaskById(taskId)).willReturn(null);

        mockMvc.perform(get("/api/tasks/id/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}