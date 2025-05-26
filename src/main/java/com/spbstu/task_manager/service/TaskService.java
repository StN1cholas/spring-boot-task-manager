package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.Task;
import com.spbstu.task_manager.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<Task> getAllTasks(Long userId) {
        return taskRepository.findByUserIdAndDeletedFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<Task> getPendingTasks(Long userId) {
        return taskRepository.findByUserIdAndDeletedFalseAndTargetDateAfter(userId, LocalDate.now());
    }

    @Transactional
    public Task createTask(Task task) {
        // Установка creationDate здесь больше не нужна, если используется @PrePersist в модели
        // Но если @PrePersist нет, то:
        // if (task.getCreationDate() == null) {
        //     task.setCreationDate(LocalDateTime.now());
        // }
        task.setDeleted(false); // Убедимся, что новая задача не удалена
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        Optional<Task> taskOptional = taskRepository.findById(id);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();
            task.setDeleted(true);
            taskRepository.save(task); // Сохраняем изменения (обновляем флаг deleted)
        }
        // Если задача не найдена, ничего не делаем или можно выбросить исключение
    }

    @Transactional(readOnly = true)
    public Task getTaskById(Long id) {
        // findById возвращает Optional, нужно его обработать
        return taskRepository.findById(id).orElse(null);
    }
}