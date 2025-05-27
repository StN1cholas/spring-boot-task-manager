package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.Task;
import com.spbstu.task_manager.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching; // Для нескольких операций с кешем
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class); // Для логгирования работы кеша
    private final TaskRepository taskRepository;

    public static final String TASKS_CACHE_NAME = "tasks"; // Имя кеша для задач по ID
    public static final String USER_TASKS_CACHE_NAME = "userTasks"; // Имя кеша для списков задач пользователя


    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    // Ключ будет userId. Если результат для этого userId уже есть в кеше "userTasks",
    // он вернется оттуда. Иначе метод выполнится, и результат закешируется.
    @Cacheable(value = USER_TASKS_CACHE_NAME, key = "#userId")
    public List<Task> getAllTasks(Long userId) {
        log.info("Fetching all tasks for user {} from DB", userId);
        return taskRepository.findByUserIdAndDeletedFalse(userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = USER_TASKS_CACHE_NAME, key = "#userId + '_pending'")
    public List<Task> getPendingTasks(Long userId) {
        log.info("Fetching pending tasks for user {} from DB", userId);
        return taskRepository.findByUserIdAndDeletedFalseAndTargetDateAfter(userId, LocalDate.now());
    }

    @Transactional
    // @CachePut всегда выполняет метод и обновляет значение в кеше.
    // Также он должен очистить списочные кеши для этого пользователя, так как списки изменились.
    @Caching(
            put = { @CachePut(value = TASKS_CACHE_NAME, key = "#result.id", condition = "#result != null && #result.id != null") },
            evict = {
                    // Попробуем с условием, что task не null
                    @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#task.userId", condition = "#task != null"),
                    @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#task.userId + '_pending'", condition = "#task != null")
            }
    )
    public Task createTask(Task task) { // Аргумент называется 'task'
        log.info("Creating task: {}", task.getTitle());
        task.setDeleted(false); // Убедимся, что новая задача не удалена
        Task savedTask = taskRepository.save(task);
        log.info("Task created with ID: {}", savedTask.getId());
        return savedTask;
    }

    @Transactional
    // При "удалении" (пометке) задачи, нужно удалить ее из кеша по ID
    // и также очистить списочные кеши для пользователя, так как его списки задач изменились.
    @Caching(
            evict = {
                    @CacheEvict(value = TASKS_CACHE_NAME, key = "#id"),
            }
    )
    public void deleteTask(Long id) {
        log.info("Deleting task with ID: {}", id);
        Optional<Task> taskOptional = taskRepository.findById(id);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();
            Long userId = task.getUserId(); // Получаем userId перед изменением
            task.setDeleted(true);
            taskRepository.save(task);
            // Теперь можно явно вызвать очистку кешей для этого пользователя
            evictUserTasksCaches(userId);
            log.info("Task with ID: {} marked as deleted", id);
        }
    }

    @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#userId")
    public void evictUserTasksCaches(Long userId) { // Должен быть public для работы @CacheEvict извне
        log.info("Evicting all tasks lists for user {}", userId);
    }

    @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#userId + '_pending'")
    public void evictUserPendingTasksCaches(Long userId) { // Должен быть public для работы @CacheEvict извне
        log.info("Evicting pending tasks list for user {}", userId);
    }

    // Переделаем deleteTask для более надежной очистки:
    @Transactional
    public Optional<Long> markTaskAsDeletedAndGetUserId(Long id) { // Возвращаем userId для использования в @CacheEvict
        log.info("Marking task with ID: {} as deleted", id);
        Optional<Task> taskOptional = taskRepository.findById(id);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();
            task.setDeleted(true);
            taskRepository.save(task);
            log.info("Task with ID: {} marked as deleted", id);
            return Optional.of(task.getUserId());
        }
        return Optional.empty();
    }

    // Обертка для контроллера, которая использует новый метод и управляет кешами
    @Caching(evict = {
            @CacheEvict(value = TASKS_CACHE_NAME, key = "#id"),
            @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#result.orElse(null)", condition = "#result.present"),
            @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#result.orElse(null) + '_pending'", condition = "#result.present")
    })
    public void deleteTaskAndEvictCaches(Long id) {

    }


    @Transactional(readOnly = true)
    // Ключ будет сам ID задачи.
    @Cacheable(value = TASKS_CACHE_NAME, key = "#id", unless = "#result == null")
    public Task getTaskById(Long id) {
        log.info("Fetching task with ID {} from DB", id);
        return taskRepository.findById(id).orElse(null);
    }
}