package com.spbstu.task_manager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Task {
    private Long id;
    private String title;
    private String description;
    private LocalDate targetDate;
    private LocalDateTime creationDate;
    private boolean deleted;
    private Long userId; // Id пользователя, которому принадлежит задача

    // Constructors, getters, setters
    public Task() {
        this.creationDate = LocalDateTime.now();
    }

    public Task(Long id, String title, String description, LocalDate targetDate, Long userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.targetDate = targetDate;
        this.creationDate = LocalDateTime.now();
        this.userId = userId;
    }

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
}