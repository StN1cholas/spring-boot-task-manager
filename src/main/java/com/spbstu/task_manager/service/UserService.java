package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(0);

    public User registerUser(User user) {
        Long id = idCounter.incrementAndGet();
        user.setId(id);
        users.put(id, user);
        return user;
    }

    public User login(String username, String password) {
        return users.values().stream()
                .filter(user -> user.getUsername().equals(username) && user.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }
}