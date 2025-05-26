package com.spbstu.task_manager.controller;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
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

    @Autowired // Внедряем мок
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
        Long userId = 1L;
        Task task1 = new Task(1L, "Task 1", "Desc 1", LocalDate.now().plusDays(1), userId);
        task1.setCreationDate(LocalDateTime.now());
        Task task2 = new Task(2L, "Task 2", "Desc 2", LocalDate.now().plusDays(2), userId);
        task2.setCreationDate(LocalDateTime.now());
        List<Task> tasks = Arrays.asList(task1, task2);

        given(taskServiceMock.getAllTasks(userId)).willReturn(tasks);

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
        Task task1 = new Task(1L, "Pending Task 1", "Desc 1", LocalDate.now().plusDays(1), userId);
        task1.setCreationDate(LocalDateTime.now());
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
        Task taskToCreate = new Task(null, "New Task", "New Desc", LocalDate.now().plusDays(3), 1L);
        Task createdTask = new Task(1L, "New Task", "New Desc", LocalDate.now().plusDays(3), 1L);
        createdTask.setCreationDate(LocalDateTime.now());

        given(taskServiceMock.createTask(any(Task.class))).willReturn(createdTask);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskToCreate)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("New Task")))
                .andExpect(jsonPath("$.targetDate").value(LocalDate.now().plusDays(3).toString()));
    }

    @Test
    void deleteTask_shouldReturnHttpStatusNoContent() throws Exception {
        Long taskId = 1L;
        doNothing().when(taskServiceMock).deleteTask(taskId);

        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isNoContent());

        verify(taskServiceMock).deleteTask(taskId);
    }

    @Test
    void getTaskById_shouldReturnTask_whenExists() throws Exception {
        Long taskId = 1L;
        Long userId = 1L;
        Task task = new Task(taskId, "Found Task", "Desc", LocalDate.now().plusDays(1), userId);
        task.setCreationDate(LocalDateTime.now());

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