package com.spbstu.task_manager.service;

import com.spbstu.task_manager.event.TaskCreatedEvent;
import com.spbstu.task_manager.model.Task;
import com.spbstu.task_manager.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult; // Для обработки результата отправки
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture; // Для асинхронной обработки результата

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private final TaskRepository taskRepository;
    private final KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate;

    @Value("${app.kafka.topic.task-created}")
    private String taskCreatedTopic;

    public static final String TASKS_CACHE_NAME = "tasks";
    public static final String USER_TASKS_CACHE_NAME = "userTasks";

    @Autowired
    public TaskService(TaskRepository taskRepository, KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate) {
        this.taskRepository = taskRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional(readOnly = true)
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
    @Caching(
            put = { @CachePut(value = TASKS_CACHE_NAME, key = "#result.id", condition = "#result != null && #result.id != null") },
            evict = {
                    // Используем #result.userId, так как это значение будет точно актуальным после сохранения
                    @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#result.userId", condition = "#result != null && #result.userId != null"),
                    @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#result.userId + '_pending'", condition = "#result != null && #result.userId != null")
            }
    )
    public Task createTask(Task task) {
        log.info("Creating task: {}", task.getTitle());
        if (task.getUserId() == null) {
            // Важно: userId должен быть установлен перед сохранением и отправкой в Kafka
            log.error("Cannot create task without userId: {}", task.getTitle());
            throw new IllegalArgumentException("Task must have a userId.");
        }
        task.setDeleted(false);
        Task savedTask = taskRepository.save(task); // @PrePersist в Task установит creationDate
        log.info("Task created with ID: {} for user ID: {}", savedTask.getId(), savedTask.getUserId());

        // Отправляем событие в Kafka после успешного сохранения задачи
        if (savedTask.getId() != null) {
            TaskCreatedEvent event = new TaskCreatedEvent(
                    savedTask.getId(),
                    savedTask.getUserId(),
                    savedTask.getTitle()
            );
            try {
                // Отправка сообщения. Ключ (здесь userId) помогает Kafka распределять сообщения по партициям.
                // Сообщения с одинаковым ключом попадут в одну и ту же партицию,
                // что гарантирует порядок обработки для этого ключа (если консьюмер один на партицию).
                CompletableFuture<SendResult<String, TaskCreatedEvent>> future =
                        kafkaTemplate.send(taskCreatedTopic, String.valueOf(savedTask.getUserId()), event);

                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Sent TaskCreatedEvent to Kafka: {} with offset: {}", event, result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send TaskCreatedEvent to Kafka: {}", event, ex);
                        // Здесь можно добавить более сложную логику обработки ошибок,
                        // например, сохранение в DLQ (Dead Letter Queue) или механизм повторных попыток.
                    }
                });
            } catch (Exception e) {
                // Эта ошибка может возникнуть, если KafkaTemplate не может даже начать отправку (например, брокер недоступен сразу)
                log.error("Error initiating send TaskCreatedEvent to Kafka: {}", event, e);
            }
        }
        return savedTask;
    }

    // Вспомогательные публичные методы для инвалидации кеша, если они нужны для вызова извне или через прокси
    // Эти методы должны быть public и, если вызываются из этого же класса, то через self-инъекцию или ApplicationContext
    // чтобы Spring AOP (для @CacheEvict) сработал.
    @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#userId")
    public void evictUserTasksCaches(Long userId) {
        log.info("Evicting all tasks lists for user {}", userId);
    }

    @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#userId + '_pending'")
    public void evictUserPendingTasksCaches(Long userId) {
        log.info("Evicting pending tasks list for user {}", userId);
    }

    @Transactional
    public Optional<Long> markTaskAsDeletedAndGetUserId(Long taskId) {
        log.info("Marking task with ID: {} as deleted", taskId);
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();
            task.setDeleted(true);
            taskRepository.save(task);
            log.info("Task with ID: {} marked as deleted for user {}", taskId, task.getUserId());
            return Optional.of(task.getUserId());
        }
        return Optional.empty();
    }

    // deleteTask должен теперь использовать markTaskAsDeletedAndGetUserId и управлять кешами
    // Аннотации @Caching лучше перенести на метод, который действительно выполняет основную логику
    // и имеет доступ ко всем нужным данным для ключей кеша.
    @Caching(evict = {
            @CacheEvict(value = TASKS_CACHE_NAME, key = "#taskId"), // Ключ - ID удаляемой задачи
            // Для списочных кешей мы используем результат выполнения markTaskAsDeletedAndGetUserId, который Optional<Long> (userId)
            @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#result.orElse(null)", condition = "#result.present"),
            @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#result.orElse(null) + '_pending'", condition = "#result.present")
    })
    public Optional<Long> deleteTask(Long taskId) { // Изменили void на Optional<Long> для возврата userId
        log.info("Attempting to delete task with ID: {}", taskId);
        // Вызываем метод, который помечает задачу как удаленную и возвращает userId
        Optional<Long> userIdOptional = markTaskAsDeletedAndGetUserId(taskId);

        if (userIdOptional.isEmpty()) {
            log.warn("Task with ID {} not found for deletion.", taskId);
        }
        // Аннотации @CacheEvict сработают после успешного выполнения этого метода,
        // используя возвращенный userId (если он есть) для ключей списочных кешей.
        // Никаких явных evictUserTasksCaches() вызовов внутри больше не нужно, если SpEL настроен правильно.
        return userIdOptional;
    }


    @Transactional(readOnly = true)
    @Cacheable(value = TASKS_CACHE_NAME, key = "#id", unless = "#result == null")
    public Task getTaskById(Long id) {
        log.info("Fetching task with ID {} from DB", id);
        return taskRepository.findById(id).orElse(null);
    }
}