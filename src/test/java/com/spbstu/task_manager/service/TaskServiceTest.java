package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.Task;
import com.spbstu.task_manager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task task1, task2;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        task1 = new Task(1L, "Task 1", "Desc 1", LocalDate.now().plusDays(1), LocalDateTime.now().minusDays(1), false, userId);
        task2 = new Task(2L, "Task 2", "Desc 2", LocalDate.now().plusDays(2), LocalDateTime.now().minusHours(5), false, userId);
    }

    @Test
    void getAllTasks_shouldReturnListOfNonDeletedTasksForUser() {
        // Arrange
        given(taskRepository.findByUserIdAndDeletedFalse(userId)).willReturn(Arrays.asList(task1, task2));

        // Act
        List<Task> tasks = taskService.getAllTasks(userId);

        // Assert
        assertThat(tasks).isNotNull().hasSize(2);
        assertThat(tasks).containsExactlyInAnyOrder(task1, task2);
        verify(taskRepository).findByUserIdAndDeletedFalse(userId);
    }

    @Test
    void getPendingTasks_shouldReturnFilteredTasks() {
        // Arrange
        LocalDate today = LocalDate.now();
        // task1.targetDate is today + 1 day, so it's pending
        given(taskRepository.findByUserIdAndDeletedFalseAndTargetDateAfter(userId, today))
                .willReturn(Collections.singletonList(task1));

        // Act
        List<Task> pendingTasks = taskService.getPendingTasks(userId);

        // Assert
        assertThat(pendingTasks).isNotNull().hasSize(1);
        assertThat(pendingTasks.get(0).getTitle()).isEqualTo("Task 1");
        verify(taskRepository).findByUserIdAndDeletedFalseAndTargetDateAfter(userId, today);
    }

    @Test
    void createTask_shouldSaveAndReturnTask() {
        // Arrange
        Task newTask = new Task("New Task", "New Desc", LocalDate.now().plusDays(5), userId);
        // @PrePersist в модели Task установит creationDate
        // Мок должен вернуть задачу так, как будто она сохранена (с ID и установленными @PrePersist полями)
        given(taskRepository.save(any(Task.class))).willAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            Task savedTask = new Task(3L, t.getTitle(), t.getDescription(), t.getTargetDate(), LocalDateTime.now(), t.isDeleted(), t.getUserId());
            if (t.getCreationDate() == null) savedTask.setCreationDate(LocalDateTime.now()); // Имитация @PrePersist
            return savedTask;
        });


        // Act
        Task createdTask = taskService.createTask(newTask);

        // Assert
        assertThat(createdTask).isNotNull();
        assertThat(createdTask.getId()).isEqualTo(3L); // Пример ID
        assertThat(createdTask.getTitle()).isEqualTo("New Task");
        assertThat(createdTask.isDeleted()).isFalse();
        assertThat(createdTask.getCreationDate()).isNotNull(); // Проверяем, что @PrePersist (или логика сервиса) сработал
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void deleteTask_shouldMarkTaskAsDeleted_whenTaskExists() {
        // Arrange
        // findById должен вернуть существующую задачу
        given(taskRepository.findById(task1.getId())).willReturn(Optional.of(task1));
        // save должен вернуть обновленную задачу (с deleted=true)
        // when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // или более точно:
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            // Убедимся, что задача, которую мы "сохраняем", имеет флаг deleted = true
            assertThat(t.isDeleted()).isTrue();
            return t;
        });


        // Act
        taskService.deleteTask(task1.getId());

        // Assert
        verify(taskRepository).findById(task1.getId());
        verify(taskRepository).save(any(Task.class)); // Проверяем, что save был вызван с измененной задачей
        // Дополнительно можно было бы проверить, что у task1 (в моке) isDeleted стало true, но это сложнее
        // с текущей настройкой. Главное - что save был вызван с объектом, у которого isDeleted=true.
    }


    @Test
    void deleteTask_shouldDoNothing_whenTaskNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        given(taskRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // Act
        taskService.deleteTask(nonExistentId);

        // Assert
        verify(taskRepository).findById(nonExistentId);
        verify(taskRepository, never()).save(any(Task.class)); // Убедимся, что save не вызывался
    }

    @Test
    void getTaskById_shouldReturnTask_whenExists() {
        // Arrange
        given(taskRepository.findById(task1.getId())).willReturn(Optional.of(task1));

        // Act
        Task foundTask = taskService.getTaskById(task1.getId());

        // Assert
        assertThat(foundTask).isNotNull();
        assertThat(foundTask.getId()).isEqualTo(task1.getId());
        verify(taskRepository).findById(task1.getId());
    }

    @Test
    void getTaskById_shouldReturnNull_whenNotExists() {
        // Arrange
        Long nonExistentId = 999L;
        given(taskRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // Act
        Task foundTask = taskService.getTaskById(nonExistentId);

        // Assert
        assertThat(foundTask).isNull();
        verify(taskRepository).findById(nonExistentId);
    }
}