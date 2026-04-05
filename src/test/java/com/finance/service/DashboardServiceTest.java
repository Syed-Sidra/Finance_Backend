package com.finance.service;

import com.finance.dto.response.DashboardResponse;
import com.finance.entity.Transaction;
import com.finance.entity.User;
import com.finance.enums.Role;
import com.finance.enums.TransactionType;
import com.finance.repository.TransactionRepository;
import com.finance.service.impl.DashboardServiceImpl;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Unit Tests")
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionServiceImpl transactionService;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private User analyst;

    @BeforeEach
    void setUp() {
        analyst = User.builder()
                .id(1L).email("analyst@finance.com")
                .role(Role.ANALYST).active(true).build();
    }

    @Test
    @DisplayName("getSummary: should compute correct totals and net balance")
    void getSummary_correctTotals() {
        when(transactionRepository.sumByType(TransactionType.INCOME))
                .thenReturn(new BigDecimal("10000.00"));
        when(transactionRepository.sumByType(TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("4000.00"));
        when(transactionRepository.sumGroupedByCategory()).thenReturn(List.of());
        when(transactionRepository.sumGroupedByCategoryAndType(TransactionType.INCOME)).thenReturn(List.of());
        when(transactionRepository.sumGroupedByCategoryAndType(TransactionType.EXPENSE)).thenReturn(List.of());
        when(transactionRepository.findRecentActivity()).thenReturn(List.of());
        when(transactionRepository.monthlyTrends()).thenReturn(List.of());

        DashboardResponse summary = dashboardService.getSummary();

        assertThat(summary.getTotalIncome()).isEqualByComparingTo("10000.00");
        assertThat(summary.getTotalExpenses()).isEqualByComparingTo("4000.00");
        assertThat(summary.getNetBalance()).isEqualByComparingTo("6000.00");
    }

    @Test
    @DisplayName("getSummary: net balance should be negative when expenses exceed income")
    void getSummary_negativeBalance() {
        when(transactionRepository.sumByType(TransactionType.INCOME))
                .thenReturn(new BigDecimal("2000.00"));
        when(transactionRepository.sumByType(TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.sumGroupedByCategory()).thenReturn(List.of());
        when(transactionRepository.sumGroupedByCategoryAndType(TransactionType.INCOME)).thenReturn(List.of());
        when(transactionRepository.sumGroupedByCategoryAndType(TransactionType.EXPENSE)).thenReturn(List.of());
        when(transactionRepository.findRecentActivity()).thenReturn(List.of());
        when(transactionRepository.monthlyTrends()).thenReturn(List.of());

        DashboardResponse summary = dashboardService.getSummary();

        assertThat(summary.getNetBalance()).isEqualByComparingTo("-3000.00");
    }

    @Test
    @DisplayName("getSummary: should populate category totals correctly")
    void getSummary_categoryTotals() {
        when(transactionRepository.sumByType(TransactionType.INCOME))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByType(TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("3500.00"));
        when(transactionRepository.sumGroupedByCategory()).thenReturn(List.of(
                new Object[]{"Rent", new BigDecimal("2000.00")},
                new Object[]{"Food", new BigDecimal("1500.00")}
        ));
        when(transactionRepository.sumGroupedByCategoryAndType(TransactionType.INCOME)).thenReturn(List.of());
        when(transactionRepository.sumGroupedByCategoryAndType(TransactionType.EXPENSE)).thenReturn(List.of(
                new Object[]{"Rent", new BigDecimal("2000.00")},
                new Object[]{"Food", new BigDecimal("1500.00")}
        ));
        when(transactionRepository.findRecentActivity()).thenReturn(List.of());
        when(transactionRepository.monthlyTrends()).thenReturn(List.of());

        DashboardResponse summary = dashboardService.getSummary();

        assertThat(summary.getCategoryTotals()).containsKeys("Rent", "Food");
        assertThat(summary.getCategoryTotals().get("Rent")).isEqualByComparingTo("2000.00");
        assertThat(summary.getExpenseCategoryTotals()).containsKeys("Rent", "Food");
    }

    @Test
    @DisplayName("getSummary: should build monthly trends correctly")
    void getSummary_monthlyTrends() {
        when(transactionRepository.sumByType(TransactionType.INCOME))
                .thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.sumByType(TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("2000.00"));
        when(transactionRepository.sumGroupedByCategory()).thenReturn(List.of());
        when(transactionRepository.sumGroupedByCategoryAndType(any())).thenReturn(List.of());
        when(transactionRepository.findRecentActivity()).thenReturn(List.of());
        when(transactionRepository.monthlyTrends()).thenReturn(List.of(
                new Object[]{2024, 6, "INCOME",  new BigDecimal("5000.00")},
                new Object[]{2024, 6, "EXPENSE", new BigDecimal("2000.00")}
        ));

        DashboardResponse summary = dashboardService.getSummary();

        assertThat(summary.getMonthlyTrends()).hasSize(1);
        DashboardResponse.MonthlyTrend trend = summary.getMonthlyTrends().get(0);
        assertThat(trend.getYear()).isEqualTo(2024);
        assertThat(trend.getMonth()).isEqualTo(6);
        assertThat(trend.getTotalIncome()).isEqualByComparingTo("5000.00");
        assertThat(trend.getTotalExpenses()).isEqualByComparingTo("2000.00");
        assertThat(trend.getNetBalance()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("getSummary: recent activity should be limited to 10")
    void getSummary_recentActivityLimit() {
        Transaction tx = Transaction.builder()
                .id(1L).amount(BigDecimal.TEN).type(TransactionType.INCOME)
                .category("Test").date(LocalDate.now()).deleted(false)
                .createdBy(analyst).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        // Return 15 transactions — should be capped at 10
        List<Transaction> fifteenTx = java.util.Collections.nCopies(15, tx);

        when(transactionRepository.sumByType(TransactionType.INCOME)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByType(TransactionType.EXPENSE)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumGroupedByCategory()).thenReturn(List.of());
        when(transactionRepository.sumGroupedByCategoryAndType(any())).thenReturn(List.of());
        when(transactionRepository.findRecentActivity()).thenReturn(fifteenTx);
        when(transactionRepository.monthlyTrends()).thenReturn(List.of());
        when(transactionService.toResponse(any())).thenCallRealMethod();

        DashboardResponse summary = dashboardService.getSummary();

        assertThat(summary.getRecentActivity()).hasSizeLessThanOrEqualTo(10);
    }
}
