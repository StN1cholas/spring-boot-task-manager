package com.spbstu.task_manager.controller;

import com.spbstu.task_manager.model.Notification;
import com.spbstu.task_manager.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Notification>> getAllNotifications(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.getAllNotifications(userId);
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }

    @GetMapping("/pending/{userId}")
    public ResponseEntity<List<Notification>> getPendingNotifications(@PathVariable Long userId) {
        List<Notification> pendingNotifications = notificationService.getPendingNotifications(userId);
        return new ResponseEntity<>(pendingNotifications, HttpStatus.OK);
    }
}