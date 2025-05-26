package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final Map<Long, Task> tasks = new ConcurrentHashMap<>(); // Потокобезопасная Map
    private final AtomicLong idCounter = new AtomicLong(0);

    public List<Task> getAllTasks(Long userId) {
        return tasks.values().stream()
                .filter(task -> !task.isDeleted() && task.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Task> getPendingTasks(Long userId) {
        return tasks.values().stream()
                .filter(task -> !task.isDeleted() && task.getUserId().equals(userId) && task.getTargetDate().isAfter(java.time.LocalDate.now()))
                .collect(Collectors.toList());
    }

    public Task createTask(Task task) {
        Long id = idCounter.incrementAndGet();
        task.setId(id);
        tasks.put(id, task);
        return task;
    }

    public void deleteTask(Long id) {
        Task task = tasks.get(id);
        if (task != null) {
            task.setDeleted(true);
        }
    }

    public Task getTaskById(Long id) {
        return tasks.get(id); // Added to get task by id
    }
}