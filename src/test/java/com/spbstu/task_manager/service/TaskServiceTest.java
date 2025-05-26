package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskServiceTest {

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService();
    }

    @Test
    void createTask_shouldAddTaskAndReturnItWithId() {
        // Arrange
        Task newTask = new Task(null, "Test Task", "Test Description", LocalDate.now().plusDays(5), 1L);

        // Act
        Task createdTask = taskService.createTask(newTask);

        // Assert
        assertThat(createdTask).isNotNull();
        assertThat(createdTask.getId()).isNotNull().isEqualTo(1L);
        assertThat(createdTask.getTitle()).isEqualTo("Test Task");
        assertThat(createdTask.getUserId()).isEqualTo(1L);
        assertThat(createdTask.isDeleted()).isFalse();
        assertThat(createdTask.getCreationDate()).isNotNull();

        Task fetchedTask = taskService.getTaskById(createdTask.getId());
        assertThat(fetchedTask).isNotNull();
        assertThat(fetchedTask.getTitle()).isEqualTo("Test Task");
    }

    @Test
    void getAllTasks_shouldReturnOnlyNonDeletedTasksForSpecificUser() {
        // Arrange
        Long userId1 = 1L;
        Long userId2 = 2L;

        Task task1User1 = taskService.createTask(new Task(null, "Task 1 U1", "Desc", LocalDate.now().plusDays(1), userId1));
        Task task2User1 = taskService.createTask(new Task(null, "Task 2 U1", "Desc", LocalDate.now().plusDays(2), userId1));
        Task deletedTaskUser1 = taskService.createTask(new Task(null, "Deleted Task U1", "Desc", LocalDate.now().plusDays(3), userId1));
        taskService.deleteTask(deletedTaskUser1.getId());
        taskService.createTask(new Task(null, "Task 1 U2", "Desc", LocalDate.now().plusDays(1), userId2)); // Task for another user

        // Act
        List<Task> user1Tasks = taskService.getAllTasks(userId1);

        // Assert
        assertThat(user1Tasks)
                .isNotNull()
                .hasSize(2)
                .extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Task 1 U1", "Task 2 U1");
    }

    @Test
    void getAllTasks_shouldReturnEmptyList_whenUserHasNoTasks() {
        // Arrange
        Long userIdWithNoTasks = 3L;
        taskService.createTask(new Task(null, "Task 1 U1", "Desc", LocalDate.now().plusDays(1), 1L)); // Add task for another user

        // Act
        List<Task> tasks = taskService.getAllTasks(userIdWithNoTasks);

        // Assert
        assertThat(tasks).isNotNull().isEmpty();
    }

    @Test
    void getPendingTasks_shouldReturnNonDeletedFutureTasksForUser() {
        // Arrange
        Long userId = 1L;
        LocalDate today = LocalDate.now();

        Task pendingTask = taskService.createTask(new Task(null, "Pending Task", "Desc", today.plusDays(1), userId)); // Future
        taskService.createTask(new Task(null, "Overdue Task", "Desc", today.minusDays(1), userId)); // Past
        taskService.createTask(new Task(null, "Task for Today", "Desc", today, userId)); // Today (not pending by current logic)
        Task deletedPendingTask = taskService.createTask(new Task(null, "Deleted Pending", "Desc", today.plusDays(2), userId));
        taskService.deleteTask(deletedPendingTask.getId());
        taskService.createTask(new Task(null, "Pending Task Other User", "Desc", today.plusDays(1), 2L)); // Pending for other user

        // Act
        List<Task> pendingTasks = taskService.getPendingTasks(userId);

        // Assert
        assertThat(pendingTasks)
                .isNotNull()
                .hasSize(1)
                .extracting(Task::getTitle)
                .containsExactly("Pending Task");
    }

    @Test
    void deleteTask_shouldMarkTaskAsDeletedAndNotReturnInAllTasks() {
        // Arrange
        Task taskToCreate = new Task(null, "Task to Delete", "Desc", LocalDate.now().plusDays(1), 1L);
        Task createdTask = taskService.createTask(taskToCreate);
        assertThat(createdTask.isDeleted()).isFalse();

        // Act
        taskService.deleteTask(createdTask.getId());

        // Assert
        Task deletedTask = taskService.getTaskById(createdTask.getId());
        assertThat(deletedTask).isNotNull();
        assertThat(deletedTask.isDeleted()).isTrue();

        List<Task> allTasks = taskService.getAllTasks(1L);
        assertThat(allTasks).noneMatch(t -> t.getId().equals(createdTask.getId()));
    }

    @Test
    void deleteTask_shouldDoNothing_whenTaskNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        Task task1 = taskService.createTask(new Task(null, "Task 1", "Desc", LocalDate.now().plusDays(1), 1L));
        long initialTaskCount = taskService.getAllTasks(1L).size();


        // Act & Assert (expecting no exception and no change)
        taskService.deleteTask(nonExistentId); // Should not throw error

        List<Task> tasksAfterDelete = taskService.getAllTasks(1L);
        assertThat(tasksAfterDelete.size()).isEqualTo(initialTaskCount);
        assertThat(task1.isDeleted()).isFalse(); // Check that other tasks are not affected
    }


    @Test
    void getTaskById_shouldReturnTask_whenExistsAndNotDeleted() {
        // Arrange
        Task taskToCreate = new Task(null, "Find Me", "Desc", LocalDate.now().plusDays(1), 1L);
        Task createdTask = taskService.createTask(taskToCreate);

        // Act
        Task foundTask = taskService.getTaskById(createdTask.getId());

        // Assert
        assertThat(foundTask).isNotNull();
        assertThat(foundTask.getId()).isEqualTo(createdTask.getId());
        assertThat(foundTask.getTitle()).isEqualTo("Find Me");
    }

    @Test
    void getTaskById_shouldReturnTask_evenIfMarkedAsDeleted() {
        // Arrange
        Task taskToCreate = new Task(null, "Find Me Deleted", "Desc", LocalDate.now().plusDays(1), 1L);
        Task createdTask = taskService.createTask(taskToCreate);
        taskService.deleteTask(createdTask.getId()); // Mark as deleted

        // Act
        Task foundTask = taskService.getTaskById(createdTask.getId());

        // Assert
        // getTaskById должен возвращать задачу, даже если она помечена как удаленная,
        // так как deleteTask не удаляет ее физически, а только ставит флаг.
        // Методы типа getAllTasks уже фильтруют по этому флагу.
        assertThat(foundTask).isNotNull();
        assertThat(foundTask.isDeleted()).isTrue();
        assertThat(foundTask.getId()).isEqualTo(createdTask.getId());
    }

    @Test
    void getTaskById_shouldReturnNull_whenNotExists() {
        // Arrange
        Long nonExistentId = 999L;

        // Act
        Task foundTask = taskService.getTaskById(nonExistentId);

        // Assert
        assertThat(foundTask).isNull();
    }
}