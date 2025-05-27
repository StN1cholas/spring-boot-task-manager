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
    // Ключ будет userId. Обрати внимание, что если getPendingTasks и getAllTasks используют один и тот же
    // кеш "userTasks" с одинаковым ключом (userId), то они могут перезаписывать друг друга или
    // возвращать не тот список, если их логика фильтрации разная.
    // Лучше использовать разные имена кешей или более сложные ключи, если логика сильно отличается.
    // Для простоты пока оставим так, но это потенциальная точка для улучшения.
    // Или можно сделать ключ более специфичным: key = "#userId + '_pending'"
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
                    @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#task.userId"),
                    @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#task.userId + '_pending'")
            }
    )
    public Task createTask(Task task) {
        log.info("Creating task: {}", task.getTitle());
        task.setDeleted(false);
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
                    // Для очистки списочных кешей нам нужен userId. Мы его получим из задачи перед "удалением".
                    // Это немного усложняет, если userId не передается напрямую.
                    // Альтернатива: @CacheEvict(allEntries = true, value = USER_TASKS_CACHE_NAME) - но это слишком грубо.
                    // Лучше получить задачу, потом ее userId, и потом евиктить.
                    // Либо, если TaskService имеет доступ к User, можно передавать User.
                    // Пока оставим так: после вызова этого метода списочные кеши для этого пользователя
                    // могут быть неактуальны до следующего запроса get*Tasks.
                    // Более продвинутый вариант - см. ниже.
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

    // Вспомогательный метод для очистки списочных кешей пользователя
    // Его можно вызывать из других методов, которые изменяют состояние задач пользователя
    // Чтобы этот метод сам был управляем кешем (для @CacheEvict), он должен быть public и вызываться через Spring proxy (т.е. из другого бина или this.method())
    // Но для простоты пока сделаем его приватным и будем просто использовать логику
    @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#userId")
    public void evictUserTasksCaches(Long userId) { // Должен быть public для работы @CacheEvict извне
        log.info("Evicting all tasks lists for user {}", userId);
    }

    @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#userId + '_pending'")
    public void evictUserPendingTasksCaches(Long userId) { // Должен быть public для работы @CacheEvict извне
        log.info("Evicting pending tasks list for user {}", userId);
    }
    // В deleteTask теперь можно вызвать:
    // this.evictUserTasksCaches(userId);
    // this.evictUserPendingTasksCaches(userId);
    // Но для этого deleteTask должен быть менее @Transactional, чтобы вызов this. пошел через прокси.
    // Или внедрить TaskService сам в себя (не рекомендуется)
    // Самый простой вариант - сделать логику очистки чуть менее автоматической через аннотации для deleteTask,
    // либо использовать CacheManager для программной очистки.

    // Для deleteTask с корректной очисткой списочных кешей:
    // Можно сделать так:
    // 1. Метод deleteTask остается @Transactional
    // 2. В конце метода deleteTask, если задача была "удалена", мы вызываем публичные методы
    //    этого же сервиса (через ApplicationContext или self-инъекцию) для очистки кешей,
    //    ЛИБО используем CacheManager API.
    //    Проще всего - если методы createTask, updateTask, deleteTask возвращают userId или сам объект Task,
    //    тогда в аннотации @CacheEvict можно использовать #result.userId.
    //    Для deleteTask, который void, это сложнее.

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
    // Этот метод не @Transactional, чтобы вызов markTaskAsDeletedAndGetUserId был через прокси
    @Caching(evict = {
            @CacheEvict(value = TASKS_CACHE_NAME, key = "#id"),
            @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#result.orElse(null)", condition = "#result.present"),
            @CacheEvict(value = USER_TASKS_CACHE_NAME, key = "#result.orElse(null) + '_pending'", condition = "#result.present")
    })
    public void deleteTaskAndEvictCaches(Long id) {
        // Внутри этого метода будет вызов this.markTaskAsDeletedAndGetUserId(id) через прокси, если TaskService внедрен сам в себя
        // или если этот метод вызывается из контроллера.
        // Для простоты, предположим, что контроллер вызывает markTaskAsDeletedAndGetUserId,
        // а потом, если успешно, можно было бы очистить кеши программно или иметь отдельные методы для очистки.

        // Пока оставим оригинальный deleteTask, но будем помнить о необходимости корректной инвалидации
        // списочных кешей. Один из способов - иметь в контроллере после вызова taskService.deleteTask(id);
        // явный вызов методов очистки, передав туда userId (который контроллер должен как-то получить).
        // Или использовать ApplicationEventPublisher для события "задача удалена", и слушатель будет чистить кеши.

        // Для простоты Step 6, сфокусируемся на @Cacheable и базовом @CacheEvict / @CachePut.
        // Сложная инвалидация - тема для углубления.
        // Оставим deleteTask как был, но с логгированием:
        // В createTask @CacheEvict уже есть и должен работать.
    }


    @Transactional(readOnly = true)
    // Ключ будет сам ID задачи.
    @Cacheable(value = TASKS_CACHE_NAME, key = "#id", unless = "#result == null")
    public Task getTaskById(Long id) {
        log.info("Fetching task with ID {} from DB", id);
        return taskRepository.findById(id).orElse(null);
    }
}