package com.spbstu.task_manager.model;

import jakarta.persistence.*; // Используем jakarta.persistence.* для JPA 3+ (Spring Boot 3+)

@Entity
@Table(name = "app_user") // "user" часто является зарезервированным словом в SQL
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Автогенерация ID базой данных
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // В реальном приложении пароль должен хешироваться

    // JPA требует конструктор без аргументов
    public User() {}

    // Конструктор без ID, так как ID будет генерироваться БД
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Конструктор для тестов или маппинга (если нужно)
    public User(Long id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }


    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}