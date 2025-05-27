package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.Notification; // Потребуется, если создаем уведомления
import com.spbstu.task_manager.model.Task;
import com.spbstu.task_manager.repository.TaskRepository; // Потребуется для поиска задач
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Если работаем с БД

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(TaskSchedulerService.class);

    private final TaskRepository taskRepository;
    private final NotificationService notificationService; // Для создания уведомлений

    @Autowired
    public TaskSchedulerService(TaskRepository taskRepository, NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRateString = "${task.scheduler.overdueCheck.rate.ms:600000}", initialDelayString = "${task.scheduler.overdueCheck.initialDelay.ms:60000}") // Каждые 10 минут, с задержкой в 1 минуту после старта
    @Transactional // Важно, если мы читаем и потом потенциально пишем (уведомления)
    public void checkOverdueTasks() {
        log.info("Scheduler: Starting check for overdue tasks...");
        LocalDate today = LocalDate.now();

        List<Task> allNonDeletedTasks = taskRepository.findAllByDeletedFalse(); // Нужен такой метод в репозитории

        int overdueCount = 0;
        for (Task task : allNonDeletedTasks) {
            if (task.getTargetDate().isBefore(today)) {
                // Задача просрочена
                log.warn("Scheduler: Task ID {} (User ID {}) titled '{}' is overdue (targetDate: {}).",
                        task.getId(), task.getUserId(), task.getTitle(), task.getTargetDate());

                // Создаем уведомление для пользователя

                String message = String.format("Внимание! Задача '%s' (ID: %d) была просрочена %s.",
                        task.getTitle(), task.getId(), task.getTargetDate().toString());
                // NotificationService уже имеет метод createNotification, который принимает Notification
                // А NotificationService.handleTaskCreatedEvent - это для Kafka.

                // В нашем NotificationService есть public Notification createNotification(Notification notification)
                // который вызывался из handleTaskCreatedEvent. Он нам подойдет.
                try {
                    Notification overdueNotification = new Notification(message, task.getUserId());
                    // timestamp установится через @PrePersist в Notification
                    notificationService.createNotification(overdueNotification); // Используем существующий метод
                    log.info("Scheduler: Created overdue notification for task ID {} for user ID {}", task.getId(), task.getUserId());
                    overdueCount++;
                } catch (Exception e) {
                    log.error("Scheduler: Failed to create overdue notification for task ID {} for user ID {}", task.getId(), task.getUserId(), e);
                }
            }
        }
        if (overdueCount > 0) {
            log.info("Scheduler: Finished check for overdue tasks. Found and processed {} overdue tasks.", overdueCount);
        } else {
            log.info("Scheduler: Finished check for overdue tasks. No new overdue tasks found to notify.");
        }
    }
}