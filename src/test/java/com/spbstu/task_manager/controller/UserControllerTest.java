package com.spbstu.task_manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spbstu.task_manager.model.User;
import com.spbstu.task_manager.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userServiceMock;

    @TestConfiguration
    static class UserControllerTestConfiguration {
        @Bean
        public UserService userService() {
            return Mockito.mock(UserService.class);
        }
    }

    @Test
    void registerUser_shouldReturnCreatedUserAndHttpStatusCreated() throws Exception {
        // Объект, который мы отправляем в запросе
        User userToRegisterRequest = new User("testuser", "password123");
        // Объект, который имитирует ответ сервиса после сохранения
        User registeredUserFromService = new User(1L, "testuser", "password123");

        given(userServiceMock.registerUser(any(User.class))).willReturn(registeredUserFromService);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userToRegisterRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1))) // is(1L) для Long
                .andExpect(jsonPath("$.username", is("testuser")));
    }

    @Test
    void login_shouldReturnUserAndHttpStatusOk_whenCredentialsAreValid() throws Exception {
        User loggedInUser = new User(1L, "testuser", "password123");
        String username = "testuser";
        String password = "password123";
        given(userServiceMock.login(username, password)).willReturn(loggedInUser);

        mockMvc.perform(get("/api/users/login")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")));
    }

    @Test
    void login_shouldReturnHttpStatusUnauthorized_whenCredentialsAreInvalid() throws Exception {
        String username = "wronguser";
        String password = "wrongpassword";
        given(userServiceMock.login(username, password)).willReturn(null);

        mockMvc.perform(get("/api/users/login")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isUnauthorized());
    }
}