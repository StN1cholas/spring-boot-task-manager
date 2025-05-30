package com.spbstu.task_manager.service;

import com.spbstu.task_manager.event.TaskCreatedEvent;
import com.spbstu.task_manager.model.Task;
import com.spbstu.task_manager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor; // Для захвата аргументов
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate; // <<< ИМПОРТ
import org.springframework.kafka.support.SendResult;  // <<< ИМПОРТ
import java.util.concurrent.CompletableFuture; // <<< ИМПОРТ


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString; // Для anyString()
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*; // Для never(), times() и т.д.

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock // <<< МОКИРУЕМ KAFKA TEMPLATE
    private KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate;

    @InjectMocks
    private TaskService taskService;

    private Task task1, task2;
    private Long userId = 1L;
    private String testTopic = "test.topic"; // Для тестов можно использовать фиктивное имя топика

    @BeforeEach
    void setUp() {
        task1 = new Task(1L, "Task 1", "Desc 1", LocalDate.now().plusDays(1), LocalDateTime.now().minusDays(1), false, userId);
        task2 = new Task(2L, "Task 2", "Desc 2", LocalDate.now().plusDays(2), LocalDateTime.now().minusHours(5), false, userId);

        // Устанавливаем значение для поля, аннотированного @Value, если оно не public
        // Это можно сделать через рефлексию или сделать поле/сеттер public/package-private для теста
        // Более простой способ, если нет рефлексии - это убедиться, что @Value не используется в логике,
        // которую мы тестируем напрямую, или что KafkaTemplate.send() правильно мокирован.
        // Для простоты, предположим, что kafkaTemplate.send() будет вызван с правильным топиком,
        // который мы можем передать в мок.
        // Если бы taskCreatedTopic был public, можно было бы: taskService.taskCreatedTopic = testTopic;
        // Или через сеттер: taskService.setTaskCreatedTopic(testTopic);
        // Или использовать ReflectionTestUtils.setField(taskService, "taskCreatedTopic", testTopic);

        // Поскольку kafkaTemplate.send использует this.taskCreatedTopic,
        // и он private, самый простой способ для юнит-теста - это при вызове verify
        // использовать ArgumentCaptor для топика или anyString(), если точное имя не критично для логики теста.
        // Для более точного теста, можно было бы сделать taskCreatedTopic доступным для установки в тесте.
        // Или в конструкторе TaskService принимать и topicName.
    }

    // ... тесты getAllTasks, getPendingTasks остаются такими же ...
    @Test
    void getAllTasks_shouldReturnListOfNonDeletedTasksForUser() {
        given(taskRepository.findByUserIdAndDeletedFalse(userId)).willReturn(Arrays.asList(task1, task2));
        List<Task> tasks = taskService.getAllTasks(userId);
        assertThat(tasks).isNotNull().hasSize(2);
        assertThat(tasks).containsExactlyInAnyOrder(task1, task2);
        verify(taskRepository).findByUserIdAndDeletedFalse(userId);
    }

    @Test
    void getPendingTasks_shouldReturnFilteredTasks() {
        LocalDate today = LocalDate.now();
        given(taskRepository.findByUserIdAndDeletedFalseAndTargetDateAfter(userId, today))
                .willReturn(Collections.singletonList(task1));
        List<Task> pendingTasks = taskService.getPendingTasks(userId);
        assertThat(pendingTasks).isNotNull().hasSize(1);
        assertThat(pendingTasks.get(0).getTitle()).isEqualTo("Task 1");
        verify(taskRepository).findByUserIdAndDeletedFalseAndTargetDateAfter(userId, today);
    }


    @Test
    void createTask_shouldSaveAndReturnTaskAndSendKafkaEvent() {
        // Arrange
        Task newTask = new Task("New Task", "New Desc", LocalDate.now().plusDays(5), userId);
        Task savedTask = new Task(3L, "New Task", "New Desc", LocalDate.now().plusDays(5),LocalDateTime.now(), false, userId);

        // Устанавливаем taskCreatedTopic для теста, если это возможно (например, через сеттер или ReflectionTestUtils)
        // ReflectionTestUtils.setField(taskService, "taskCreatedTopic", "test.topic");
        // Если нет, то при verify используем anyString() для имени топика.

        given(taskRepository.save(any(Task.class))).willReturn(savedTask);

        // Настраиваем мок KafkaTemplate.send()
        // CompletableFuture успешно завершенный
        //CompletableFuture<SendResult<String, TaskCreatedEvent>> completableFuture = new CompletableFuture<>();

        @SuppressWarnings("unchecked") // Можно добавить, если IDE все еще ругается на unchecked cast
        SendResult<String, TaskCreatedEvent> mockedSendResult = mock(SendResult.class);

        CompletableFuture<SendResult<String, TaskCreatedEvent>> completableFuture = CompletableFuture.completedFuture(mockedSendResult);

        given(kafkaTemplate.send(anyString(), anyString(), any(TaskCreatedEvent.class)))
                .willReturn(completableFuture); // Возвращаем CompletableFuture, чтобы не было NPE на whenComplete

        // Act
        Task createdTask = taskService.createTask(newTask);

        // Assert
        assertThat(createdTask).isNotNull();
        assertThat(createdTask.getId()).isEqualTo(3L);
        verify(taskRepository).save(any(Task.class));

        // Проверяем, что kafkaTemplate.send был вызван
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TaskCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCreatedEvent.class);

        // Используем anyString() для topic, если не можем установить taskCreatedTopic в тесте
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        // assertThat(topicCaptor.getValue()).isEqualTo(testTopic); // Если установили testTopic
        assertThat(keyCaptor.getValue()).isEqualTo(String.valueOf(userId));
        assertThat(eventCaptor.getValue().getTaskId()).isEqualTo(savedTask.getId());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().getTaskTitle()).isEqualTo(savedTask.getTitle());

        // Имитируем успешное завершение future, чтобы покрыть логгирование
        completableFuture.complete(mock(SendResult.class));
    }

    @Test
    void deleteTask_shouldMarkTaskAsDeletedAndEvictCaches_whenTaskExists() {
        // Arrange
        Task taskToDelete = new Task(1L, "Task to delete", "Desc", LocalDate.now(), LocalDateTime.now(), false, userId);
        given(taskRepository.findById(1L)).willReturn(Optional.of(taskToDelete));
        given(taskRepository.save(any(Task.class))).willAnswer(inv -> inv.getArgument(0)); // Просто возвращаем то, что передали

        // Act
        Optional<Long> deletedTaskUserId = taskService.deleteTask(1L);

        // Assert
        assertThat(deletedTaskUserId).isPresent().contains(userId);
        verify(taskRepository).findById(1L);
        verify(taskRepository).save(argThat(task -> task.isDeleted() && task.getId().equals(1L)));
    }

    @Test
    void deleteTask_shouldReturnEmpty_whenTaskNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        given(taskRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // Act
        Optional<Long> result = taskService.deleteTask(nonExistentId);

        // Assert
        assertThat(result).isEmpty();
        verify(taskRepository).findById(nonExistentId);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void getTaskById_shouldReturnTask_whenExists() {
        given(taskRepository.findById(task1.getId())).willReturn(Optional.of(task1));
        Task foundTask = taskService.getTaskById(task1.getId());
        assertThat(foundTask).isNotNull();
        assertThat(foundTask.getId()).isEqualTo(task1.getId());
        verify(taskRepository).findById(task1.getId());
    }

    @Test
    void getTaskById_shouldReturnNull_whenNotExists() {
        Long nonExistentId = 999L;
        given(taskRepository.findById(nonExistentId)).willReturn(Optional.empty());
        Task foundTask = taskService.getTaskById(nonExistentId);
        assertThat(foundTask).isNull();
        verify(taskRepository).findById(nonExistentId);
    }
}