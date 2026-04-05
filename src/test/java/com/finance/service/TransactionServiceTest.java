package com.finance.service;

import com.finance.dto.request.TransactionRequest;
import com.finance.dto.response.TransactionResponse;
import com.finance.entity.Transaction;
import com.finance.entity.User;
import com.finance.enums.Role;
import com.finance.enums.TransactionType;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.TransactionRepository;
import com.finance.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User currentUser;
    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .email("analyst@finance.com")
                .role(Role.ANALYST)
                .active(true)
                .build();

        sampleTransaction = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("5000.00"))
                .type(TransactionType.INCOME)
                .category("Salary")
                .date(LocalDate.of(2024, 6, 1))
                .notes("Monthly salary")
                .deleted(false)
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- createTransaction ---

    @Test
    @DisplayName("createTransaction: should save and return transaction")
    void createTransaction_success() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("5000.00"));
        request.setType(TransactionType.INCOME);
        request.setCategory("Salary");
        request.setDate(LocalDate.of(2024, 6, 1));
        request.setNotes("Monthly salary");

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        TransactionResponse response = transactionService.createTransaction(request);

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(response.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(response.getCategory()).isEqualTo("Salary");
        assertThat(response.getCreatedByEmail()).isEqualTo("analyst@finance.com");
        verify(transactionRepository).save(any(Transaction.class));
    }

    // --- getTransactionById ---

    @Test
    @DisplayName("getTransactionById: should return transaction when found")
    void getTransactionById_found() {
        when(transactionRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(sampleTransaction));

        TransactionResponse response = transactionService.getTransactionById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCategory()).isEqualTo("Salary");
    }

    @Test
    @DisplayName("getTransactionById: should throw ResourceNotFoundException when not found or deleted")
    void getTransactionById_notFound() {
        when(transactionRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found with id: 99");
    }

    // --- getAllTransactions ---

    @Test
    @DisplayName("getAllTransactions: no filters returns all non-deleted")
    void getAllTransactions_noFilters() {
        when(transactionRepository.findByDeletedFalse()).thenReturn(List.of(sampleTransaction));

        List<TransactionResponse> results =
                transactionService.getAllTransactions(null, null, null, null);

        assertThat(results).hasSize(1);
        verify(transactionRepository).findByDeletedFalse();
    }

    @Test
    @DisplayName("getAllTransactions: filter by type")
    void getAllTransactions_filterByType() {
        when(transactionRepository.findByDeletedFalseAndType(TransactionType.INCOME))
                .thenReturn(List.of(sampleTransaction));

        List<TransactionResponse> results =
                transactionService.getAllTransactions(TransactionType.INCOME, null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo(TransactionType.INCOME);
        verify(transactionRepository).findByDeletedFalseAndType(TransactionType.INCOME);
    }

    @Test
    @DisplayName("getAllTransactions: filter by category")
    void getAllTransactions_filterByCategory() {
        when(transactionRepository.findByDeletedFalseAndCategoryIgnoreCase("Salary"))
                .thenReturn(List.of(sampleTransaction));

        List<TransactionResponse> results =
                transactionService.getAllTransactions(null, "Salary", null, null);

        assertThat(results).hasSize(1);
        verify(transactionRepository).findByDeletedFalseAndCategoryIgnoreCase("Salary");
    }

    @Test
    @DisplayName("getAllTransactions: filter by date range")
    void getAllTransactions_filterByDateRange() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 6, 30);

        when(transactionRepository.findByDeletedFalseAndDateBetween(from, to))
                .thenReturn(List.of(sampleTransaction));

        List<TransactionResponse> results =
                transactionService.getAllTransactions(null, null, from, to);

        assertThat(results).hasSize(1);
        verify(transactionRepository).findByDeletedFalseAndDateBetween(from, to);
    }

    @Test
    @DisplayName("getAllTransactions: filter by type and date range")
    void getAllTransactions_filterByTypeAndDateRange() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 6, 30);

        when(transactionRepository.findByDeletedFalseAndTypeAndDateBetween(
                TransactionType.INCOME, from, to))
                .thenReturn(List.of(sampleTransaction));

        List<TransactionResponse> results =
                transactionService.getAllTransactions(TransactionType.INCOME, null, from, to);

        assertThat(results).hasSize(1);
        verify(transactionRepository).findByDeletedFalseAndTypeAndDateBetween(
                TransactionType.INCOME, from, to);
    }

    // --- updateTransaction ---

    @Test
    @DisplayName("updateTransaction: should update and return updated transaction")
    void updateTransaction_success() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("6000.00"));
        request.setType(TransactionType.INCOME);
        request.setCategory("Bonus");
        request.setDate(LocalDate.of(2024, 6, 15));
        request.setNotes("Performance bonus");

        Transaction updated = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("6000.00"))
                .type(TransactionType.INCOME)
                .category("Bonus")
                .date(LocalDate.of(2024, 6, 15))
                .notes("Performance bonus")
                .deleted(false)
                .createdBy(currentUser)
                .createdAt(sampleTransaction.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(sampleTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(updated);

        TransactionResponse response = transactionService.updateTransaction(1L, request);

        assertThat(response.getAmount()).isEqualByComparingTo("6000.00");
        assertThat(response.getCategory()).isEqualTo("Bonus");
    }

    // --- deleteTransaction ---

    @Test
    @DisplayName("deleteTransaction: should soft-delete (set deleted = true)")
    void deleteTransaction_softDelete() {
        when(transactionRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(sampleTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        transactionService.deleteTransaction(1L);

        assertThat(sampleTransaction.isDeleted()).isTrue();
        verify(transactionRepository).save(sampleTransaction);
    }

    @Test
    @DisplayName("deleteTransaction: should throw ResourceNotFoundException for missing transaction")
    void deleteTransaction_notFound() {
        when(transactionRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).save(any());
    }
}
