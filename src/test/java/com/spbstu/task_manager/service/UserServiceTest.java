package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
    }

    @Test
    void registerUser_shouldAddUserAndReturnItWithId() {
        // Arrange
        User newUser = new User(null, "testuser", "password123");

        // Act
        User registeredUser = userService.registerUser(newUser);

        // Assert
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getId()).isNotNull().isEqualTo(1L); // Первый пользователь получит ID 1
        assertThat(registeredUser.getUsername()).isEqualTo("testuser");
        assertThat(registeredUser.getPassword()).isEqualTo("password123"); // В реальном приложении пароль бы хешировался

        // Проверяем, что пользователь действительно добавлен
        User foundUser = userService.login("testuser", "password123");
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(registeredUser.getId());
    }

    @Test
    void registerUser_shouldAssignIncrementingIds() {
        // Arrange
        User user1 = new User(null, "user1", "pass1");
        User user2 = new User(null, "user2", "pass2");

        // Act
        User registeredUser1 = userService.registerUser(user1);
        User registeredUser2 = userService.registerUser(user2);

        // Assert
        assertThat(registeredUser1.getId()).isEqualTo(1L);
        assertThat(registeredUser2.getId()).isEqualTo(2L);
    }

    @Test
    void login_shouldReturnUser_whenCredentialsAreCorrect() {
        // Arrange
        User userToRegister = new User(null, "correctuser", "correctpass");
        userService.registerUser(userToRegister);

        // Act
        User loggedInUser = userService.login("correctuser", "correctpass");

        // Assert
        assertThat(loggedInUser).isNotNull();
        assertThat(loggedInUser.getUsername()).isEqualTo("correctuser");
    }

    @Test
    void login_shouldReturnNull_whenUsernameIsIncorrect() {
        // Arrange
        userService.registerUser(new User(null, "someuser", "somepass"));

        // Act
        User loggedInUser = userService.login("wronguser", "somepass");

        // Assert
        assertThat(loggedInUser).isNull();
    }

    @Test
    void login_shouldReturnNull_whenPasswordIsIncorrect() {
        // Arrange
        userService.registerUser(new User(null, "someuser", "somepass"));

        // Act
        User loggedInUser = userService.login("someuser", "wrongpass");

        // Assert
        assertThat(loggedInUser).isNull();
    }

    @Test
    void login_shouldReturnNull_whenUserDoesNotExist() {
        // Act
        User loggedInUser = userService.login("nonexistent", "nopass");

        // Assert
        assertThat(loggedInUser).isNull();
    }
}