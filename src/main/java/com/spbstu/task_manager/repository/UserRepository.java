package com.spbstu.task_manager.repository;

import com.spbstu.task_manager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository<User, Long> - User это тип сущности, Long это тип ее ID

    // Spring Data JPA автоматически сгенерирует реализацию этого метода
    // на основе его имени.
    Optional<User> findByUsername(String username);

    // Для логина можно использовать findByUsername и потом проверять пароль в сервисе
    // или создать более сложный запрос, если бы пароль хешировался в БД.
    // Пока findByUsername достаточно.
    Optional<User> findByUsernameAndPassword(String username, String password); // Для текущей простой логики логина
}