package com.spbstu.task_manager.controller;

import com.spbstu.task_manager.model.User;
import com.spbstu.task_manager.service.NotificationService;
import com.spbstu.task_manager.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class); // <<< ДОБАВИТЬ ЛОГГЕР

    private final UserService userService;
    private final NotificationService notificationService; // <<< ДОБАВИТЬ ЗАВИСИМОСТЬ

    @Autowired
    public UserController(UserService userService, NotificationService notificationService) { // <<< ИЗМЕНИТЬ КОНСТРУКТОР
        this.userService = userService;
        this.notificationService = notificationService; // <<< ИНИЦИАЛИЗИРОВАТЬ ЗАВИСИМОСТЬ
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        User registeredUser = userService.registerUser(user);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    @GetMapping("/login")
    public ResponseEntity<User> login(@RequestParam String username, @RequestParam String password) {
        User user = userService.login(username, password);
        if (user != null) {
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    // ВРЕМЕННЫЙ ЭНДПОИНТ ДЛЯ ТЕСТИРОВАНИЯ @Async
    @GetMapping("/test-async")
    public ResponseEntity<String> triggerTestAsyncOperation() {
        log.info("UserController: Received request for /test-async. Current thread: {}", Thread.currentThread().getName());
        notificationService.performBackgroundProcessing("Test data from UserController for async processing");
        log.info("UserController: Async operation (performBackgroundProcessing) initiated. Returning response from /test-async. Current thread: {}", Thread.currentThread().getName());
        return ResponseEntity.ok("Async operation initiated. Check application logs for details from NotificationService.");
    }
}