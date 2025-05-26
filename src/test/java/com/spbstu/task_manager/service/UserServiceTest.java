package com.spbstu.task_manager.service;

import com.spbstu.task_manager.model.User;
import com.spbstu.task_manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // Для использования @Mock и @InjectMocks без Spring контекста
public class UserServiceTest {

    @Mock // Mockito создаст мок для UserRepository
    private UserRepository userRepository;

    @InjectMocks // Mockito создаст экземпляр UserService и внедрит в него мок userRepository
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(1L, "testuser", "password123");
    }

    @Test
    void registerUser_shouldSaveAndReturnUser_whenUsernameIsUnique() {
        // Arrange
        User newUser = new User("newuser", "newpass");
        // Настроить мок userRepository.findByUsername(): должен вернуть empty, т.к. юзер новый
        given(userRepository.findByUsername(newUser.getUsername())).willReturn(Optional.empty());
        // Настроить мок userRepository.save(): должен вернуть пользователя с присвоенным ID
        // Важно: any(User.class) здесь, т.к. объект user передаваемый в save может отличаться от newUser (например, если бы мы хешировали пароль)
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User u = invocation.getArgument(0);
            // Имитируем присвоение ID базой данных
            if (u.getId() == null) { // Только если ID еще не присвоен
                // В реальном репозитории это делает БД, здесь мы просто возвращаем объект как есть,
                // так как в UserService.registerUser мы передаем user в save.
                // Если бы ID генерировался иначе, логика мока была бы сложнее.
                // Для простоты, будем считать что save() возвращает объект с ID (если он был null).
                // Более точно было бы вернуть копию с присвоенным ID.
                // Но для данного теста важно, что save был вызван.
                User savedUserWithId = new User(1L, u.getUsername(), u.getPassword()); // Пример
                return savedUserWithId;
            }
            return u;
        });


        // Act
        User registeredUser = userService.registerUser(newUser);

        // Assert
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getUsername()).isEqualTo("newuser");
        assertThat(registeredUser.getId()).isNotNull(); // Проверяем, что ID присвоен (имитировано моком)
        verify(userRepository).findByUsername(newUser.getUsername()); // Проверяем, что findByUsername был вызван
        verify(userRepository).save(any(User.class)); // Проверяем, что save был вызван
    }

    @Test
    void registerUser_shouldThrowException_whenUsernameExists() {
        // Arrange
        User existingUser = new User("existinguser", "pass");
        given(userRepository.findByUsername(existingUser.getUsername())).willReturn(Optional.of(existingUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(existingUser);
        });
        assertThat(exception.getMessage()).contains("already exists");
        verify(userRepository).findByUsername(existingUser.getUsername());
        verify(userRepository, never()).save(any(User.class)); // Убедимся, что save не вызывался
    }

    @Test
    void login_shouldReturnUser_whenCredentialsAreCorrect() {
        // Arrange
        given(userRepository.findByUsernameAndPassword(user.getUsername(), user.getPassword()))
                .willReturn(Optional.of(user));

        // Act
        User loggedInUser = userService.login(user.getUsername(), user.getPassword());

        // Assert
        assertThat(loggedInUser).isNotNull();
        assertThat(loggedInUser.getUsername()).isEqualTo(user.getUsername());
        verify(userRepository).findByUsernameAndPassword(user.getUsername(), user.getPassword());
    }

    @Test
    void login_shouldReturnNull_whenCredentialsAreIncorrect() {
        // Arrange
        given(userRepository.findByUsernameAndPassword(anyString(), anyString()))
                .willReturn(Optional.empty());

        // Act
        User loggedInUser = userService.login("wronguser", "wrongpass");

        // Assert
        assertThat(loggedInUser).isNull();
        verify(userRepository).findByUsernameAndPassword("wronguser", "wrongpass");
    }
}