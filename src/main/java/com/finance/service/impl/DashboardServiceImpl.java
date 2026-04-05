package com.finance.service.impl;

import com.finance.dto.response.DashboardResponse;
import com.finance.dto.response.TransactionResponse;
import com.finance.enums.TransactionType;
import com.finance.repository.TransactionRepository;
import com.finance.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TransactionRepository transactionRepository;
    private final TransactionServiceImpl transactionService;

    private static final int RECENT_ACTIVITY_LIMIT = 10;

    @Override
    public DashboardResponse getSummary() {
        BigDecimal totalIncome = transactionRepository.sumByType(TransactionType.INCOME);
        BigDecimal totalExpenses = transactionRepository.sumByType(TransactionType.EXPENSE);
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        Map<String, BigDecimal> categoryTotals = buildCategoryMap(
                transactionRepository.sumGroupedByCategory());

        Map<String, BigDecimal> incomeCategoryTotals = buildCategoryMap(
                transactionRepository.sumGroupedByCategoryAndType(TransactionType.INCOME));
        Map<String, BigDecimal> expenseCategoryTotals = buildCategoryMap(
                transactionRepository.sumGroupedByCategoryAndType(TransactionType.EXPENSE));

        List<TransactionResponse> recentActivity = transactionRepository
                .findRecentActivity()
                .stream()
                .limit(RECENT_ACTIVITY_LIMIT)
                .map(transactionService::toResponse)
                .collect(Collectors.toList());

        List<DashboardResponse.MonthlyTrend> monthlyTrends = buildMonthlyTrends(
                transactionRepository.monthlyTrends());

        return DashboardResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .categoryTotals(categoryTotals)
                .incomeCategoryTotals(incomeCategoryTotals)
                .expenseCategoryTotals(expenseCategoryTotals)
                .recentActivity(recentActivity)
                .monthlyTrends(monthlyTrends)
                .build();
    }

    private Map<String, BigDecimal> buildCategoryMap(List<Object[]> rows) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String category = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            map.put(category, total);
        }
        return map;
    }

    private List<DashboardResponse.MonthlyTrend> buildMonthlyTrends(List<Object[]> rows) {

        Map<String, DashboardResponse.MonthlyTrend> trendMap = new LinkedHashMap<>();

        for (Object[] row : rows) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            TransactionType type = TransactionType.valueOf(row[2].toString());
            BigDecimal sum = (BigDecimal) row[3];

            String key = year + "-" + String.format("%02d", month);

            DashboardResponse.MonthlyTrend trend = trendMap.computeIfAbsent(key, k ->
                    DashboardResponse.MonthlyTrend.builder()
                            .year(year)
                            .month(month)
                            .totalIncome(BigDecimal.ZERO)
                            .totalExpenses(BigDecimal.ZERO)
                            .netBalance(BigDecimal.ZERO)
                            .build());

            if (type == TransactionType.INCOME) {
                trend.setTotalIncome(sum);
            } else {
                trend.setTotalExpenses(sum);
            }
            trend.setNetBalance(trend.getTotalIncome().subtract(trend.getTotalExpenses()));
        }

        return new ArrayList<>(trendMap.values());
    }
}
