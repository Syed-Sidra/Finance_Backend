package com.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dto.request.LoginRequest;
import com.finance.entity.User;
import com.finance.enums.Role;
import com.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User admin = User.builder()
                .fullName("Admin User")
                .email("admin@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(admin);
    }

    @Test
    @DisplayName("POST /api/auth/login - success returns JWT token")
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.email").value("admin@test.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/auth/login - wrong password returns 401")
    void login_wrongPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/login - blank email fails validation with 400")
    void login_missingEmail_validationFails() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("POST /api/auth/login - deactivated user returns 401")
    void login_inactiveUser_returns401() throws Exception {
        userRepository.deleteAll();
        User inactive = User.builder()
                .fullName("Inactive User")
                .email("inactive@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.VIEWER)
                .active(false)
                .build();
        userRepository.save(inactive);

        LoginRequest request = new LoginRequest();
        request.setEmail("inactive@test.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
