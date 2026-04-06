package com.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dto.request.TransactionRequest;
import com.finance.entity.User;
import com.finance.enums.Role;
import com.finance.enums.TransactionType;
import com.finance.repository.TransactionRepository;
import com.finance.repository.UserRepository;
import com.finance.security.JwtUtils;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TransactionController Integration Tests")
class TransactionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    private String adminToken;
    private String viewerToken;
    private String analystToken;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        userRepository.deleteAll();

        adminToken  = createUserAndToken("admin@test.com",   "Admin",   Role.ADMIN);
        analystToken = createUserAndToken("analyst@test.com", "Analyst", Role.ANALYST);
        viewerToken = createUserAndToken("viewer@test.com",  "Viewer",  Role.VIEWER);
    }

    private String createUserAndToken(String email, String name, Role role) {
        User user = userRepository.save(User.builder()
                .fullName(name).email(email)
                .password(passwordEncoder.encode("password123"))
                .role(role).active(true).build());
        return "Bearer " + jwtUtils.generateAccessToken(user);
    }

    private TransactionRequest validRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("1500.00"));
        req.setType(TransactionType.INCOME);
        req.setCategory("Freelance");
        req.setDate(LocalDate.of(2024, 6, 10));
        req.setNotes("Project payment");
        return req;
    }

    // --- Create ---

    @Test
    @DisplayName("POST /api/transactions - ADMIN can create transaction")
    void createTransaction_adminAllowed() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.category").value("Freelance"))
                .andExpect(jsonPath("$.data.amount").value(1500.00));
    }

    @Test
    @DisplayName("POST /api/transactions - ANALYST can create transaction")
    void createTransaction_analystAllowed() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/transactions - VIEWER is forbidden")
    void createTransaction_viewerForbidden() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/transactions - unauthenticated request returns 401")
    void createTransaction_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/transactions - invalid amount returns 400")
    void createTransaction_invalidAmount() throws Exception {
        TransactionRequest bad = validRequest();
        bad.setAmount(new BigDecimal("-100"));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/transactions - missing category returns 400")
    void createTransaction_missingCategory() throws Exception {
        TransactionRequest bad = validRequest();
        bad.setCategory("");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    // --- Read ---

    @Test
    @DisplayName("GET /api/transactions - VIEWER can read all transactions")
    void getAllTransactions_viewerAllowed() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/transactions?type=INCOME - filter by type works")
    void getAllTransactions_filterByType() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("type", "INCOME")
                        .header("Authorization", viewerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/transactions?from=2024-01-01&to=2024-12-31 - date filter works")
    void getAllTransactions_filterByDateRange() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("from", "2024-01-01")
                        .param("to", "2024-12-31")
                        .header("Authorization", viewerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/transactions - invalid date range (from > to) returns 400")
    void getAllTransactions_invalidDateRange() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("from", "2024-12-31")
                        .param("to", "2024-01-01")
                        .header("Authorization", viewerToken))
                .andExpect(status().isBadRequest());
    }

    // --- Delete ---

    @Test
    @DisplayName("DELETE /api/transactions/{id} - VIEWER cannot delete")
    void deleteTransaction_viewerForbidden() throws Exception {
        mockMvc.perform(delete("/api/transactions/1")
                        .header("Authorization", viewerToken))
                .andExpect(status().isForbidden());
    }
}
