package com.spbstu.task_manager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Long userId; // Пока просто ID пользователя. Позже можно сделать @ManyToOne связь

    public Notification() {
        // timestamp будет устанавливаться в сервисе перед сохранением или через @PrePersist
    }

    // Конструктор без ID
    public Notification(String message, Long userId) {
        this.message = message;
        this.userId = userId;
        // timestamp будет устанавливаться в сервисе перед сохранением или через @PrePersist
    }

    // Полный конструктор (может быть полезен для маппинга или тестов)
    public Notification(Long id, String message, LocalDateTime timestamp, Long userId) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.userId = userId;
    }


    // Getters and Setters
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

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}