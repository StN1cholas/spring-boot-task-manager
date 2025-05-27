package com.spbstu.task_manager.event;

import java.io.Serializable; // Для JsonSerializer может не требоваться, но хорошая практика
// Если используешь стандартный Kafka AvroSerializer/Deserializer, то там свои правила

// Lombok аннотации для boilerplate кода (getters, setters, constructor)
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//@Data
//@NoArgsConstructor
//@AllArgsConstructor
public class TaskCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private Long userId;
    private String taskTitle;
    // Можно добавить и другие поля, если они нужны NotificationService

    public TaskCreatedEvent() {
    }

    public TaskCreatedEvent(Long taskId, Long userId, String taskTitle) {
        this.taskId = taskId;
        this.userId = userId;
        this.taskTitle = taskTitle;
    }

    // Getters and Setters
    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    @Override
    public String toString() {
        return "TaskCreatedEvent{" +
                "taskId=" + taskId +
                ", userId=" + userId +
                ", taskTitle='" + taskTitle + '\'' +
                '}';
    }
}