package com.spbstu.task_manager.model;

import java.time.LocalDateTime;

public class Notification {
    private Long id;
    private String message;
    private LocalDateTime timestamp;
    private Long userId; // Id пользователя, которому принадлежит уведомление

    // Constructors, getters, setters
    public Notification() {
        this.timestamp = LocalDateTime.now();
    }

    public Notification(Long id, String message, Long userId) {
        this.id = id;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}