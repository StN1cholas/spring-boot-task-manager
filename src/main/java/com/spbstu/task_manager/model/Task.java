package com.spbstu.task_manager.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "task")
public class Task implements Serializable{

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description; // Может быть nullable

    @Column(nullable = false)
    private LocalDate targetDate;

    @Column(nullable = false, updatable = false) // updatable = false, т.к. дата создания не должна меняться
    private LocalDateTime creationDate;

    private boolean deleted = false; // Значение по умолчанию

    @Column(nullable = false)
    private Long userId; // Пока просто ID пользователя. Позже можно сделать @ManyToOne связь

    public Task() {
        // creationDate будет устанавливаться в сервисе перед сохранением
    }

    // Конструктор без ID
    public Task(String title, String description, LocalDate targetDate, Long userId) {
        this.title = title;
        this.description = description;
        this.targetDate = targetDate;
        this.userId = userId;
        // creationDate будет устанавливаться в сервисе перед сохранением
    }

    // Полный конструктор (может быть полезен для маппинга или тестов)
    public Task(Long id, String title, String description, LocalDate targetDate, LocalDateTime creationDate, boolean deleted, Long userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.targetDate = targetDate;
        this.creationDate = creationDate;
        this.deleted = deleted;
        this.userId = userId;
    }


    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    // Метод для установки даты создания перед сохранением в БД
    @PrePersist
    protected void onCreate() {
        if (this.creationDate == null) { // Устанавливаем только если еще не установлена
            this.creationDate = LocalDateTime.now();
        }
    }
}