package com.finance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;

    private Map<String, BigDecimal> categoryTotals;
    private Map<String, BigDecimal> incomeCategoryTotals;
    private Map<String, BigDecimal> expenseCategoryTotals;

    private List<TransactionResponse> recentActivity;
    private List<MonthlyTrend> monthlyTrends;

    @Data
    @Builder
    public static class MonthlyTrend {
        private int year;
        private int month;
        private BigDecimal totalIncome;
        private BigDecimal totalExpenses;
        private BigDecimal netBalance;
    }
}
