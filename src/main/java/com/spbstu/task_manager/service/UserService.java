package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.User;
import com.spbstu.task_manager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Для транзакций

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional // Регистрация - это операция изменения данных
    public User registerUser(User user) {
        // Можно добавить проверку, не существует ли уже пользователь с таким username
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            // Здесь можно выбросить исключение или вернуть null/специальный ответ
            throw new IllegalArgumentException("User with username " + user.getUsername() + " already exists.");
        }
        // В реальном приложении здесь бы хешировался пароль перед сохранением
        // user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional(readOnly = true) // Логин - это операция чтения
    public User login(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsernameAndPassword(username, password);
        // В реальном приложении:
        // 1. Найти пользователя по username: userRepository.findByUsername(username)
        // 2. Если найден, сравнить хеш пароля: passwordEncoder.matches(password, user.getPassword())
        return userOptional.orElse(null);
    }
}