package com.finance.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dto.request.LoginRequest;
import com.finance.dto.request.RefreshRequest;
import com.finance.entity.User;
import com.finance.enums.Role;
import com.finance.repository.RefreshTokenRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security & RBAC Integration Tests")
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        createUser("admin@test.com", Role.ADMIN);
        createUser("analyst@test.com", Role.ANALYST);
        createUser("viewer@test.com", Role.VIEWER);
    }

    private void createUser(String email, Role role) {
        userRepository.save(User.builder()
                .email(email).fullName("Test " + role.name())
                .password(passwordEncoder.encode("pass123"))
                .role(role).active(true).build());
    }

    private String loginAndGetToken(String email) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword("pass123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    // ── Login tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login: valid credentials → 200 with access and refresh token")
    void login_validCredentials_returnsTokens() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("pass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Login: wrong password → 401 INVALID_CREDENTIALS")
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("INVALID_CREDENTIALS")));
    }

    // ── Token refresh tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Refresh: valid refresh token → new access + refresh tokens")
    void refresh_validToken_returnsNewTokens() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("admin@test.com");
        loginReq.setPassword("pass123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn();

        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        RefreshRequest refreshReq = new RefreshRequest();
        refreshReq.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Refresh: invalid token → 404 not found")
    void refresh_invalidToken_returns404() throws Exception {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("completely-invalid-token-value");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ── RBAC: unauthenticated ────────────────────────────────────────────────

    @Test
    @DisplayName("No token: protected endpoint → 401")
    void noToken_protectedEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    // ── RBAC: VIEWER ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("VIEWER: GET /api/transactions → 200")
    void viewer_getTransactions_returns200() throws Exception {
        String token = loginAndGetToken("viewer@test.com");
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("VIEWER: POST /api/transactions → 403")
    void viewer_createTransaction_returns403() throws Exception {
        String token = loginAndGetToken("viewer@test.com");
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100,\"type\":\"INCOME\",\"category\":\"Test\",\"date\":\"2024-01-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("VIEWER: GET /api/users → 403")
    void viewer_getUsers_returns403() throws Exception {
        String token = loginAndGetToken("viewer@test.com");
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ── RBAC: ANALYST ────────────────────────────────────────────────────────

    @Test
    @DisplayName("ANALYST: POST /api/transactions → 201")
    void analyst_createTransaction_returns201() throws Exception {
        String token = loginAndGetToken("analyst@test.com");
        String body = "{\"amount\":1500.00,\"type\":\"INCOME\",\"category\":\"Freelance\",\"date\":\"2024-06-01\"}";

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category").value("Freelance"));
    }

    @Test
    @DisplayName("ANALYST: GET /api/users → 403")
    void analyst_getUsers_returns403() throws Exception {
        String token = loginAndGetToken("analyst@test.com");
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ── RBAC: ADMIN ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN: GET /api/users → 200")
    void admin_getUsers_returns200() throws Exception {
        String token = loginAndGetToken("admin@test.com");
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("ADMIN: GET /api/dashboard/summary → 200")
    void admin_getDashboard_returns200() throws Exception {
        String token = loginAndGetToken("admin@test.com");
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalIncome").exists())
                .andExpect(jsonPath("$.data.totalExpenses").exists())
                .andExpect(jsonPath("$.data.netBalance").exists());
    }

    // ── Response consistency ──────────────────────────────────────────────────

    @Test
    @DisplayName("All responses include success, message, timestamp fields")
    void response_hasConsistentEnvelope() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("pass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Error response includes error code in message")
    void errorResponse_includesErrorCode() throws Exception {
        mockMvc.perform(get("/api/transactions/99999")
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("X-Request-ID header is present in every response")
    void response_hasRequestIdHeader() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com")))
                .andExpect(header().exists("X-Request-ID"));
    }

    // ── Validation ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Validation: missing required fields → 400 with field errors")
    void validation_missingFields_returns400() throws Exception {
        String token = loginAndGetToken("admin@test.com");
        String badBody = "{\"amount\":-1,\"type\":\"INCOME\"}";

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").isMap());
    }
}
