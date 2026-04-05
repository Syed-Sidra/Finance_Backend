package com.finance.repository;

import com.finance.entity.Transaction;
import com.finance.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByDeletedFalse();

    Optional<Transaction> findByIdAndDeletedFalse(Long id);

    List<Transaction> findByDeletedFalseAndType(TransactionType type);

    List<Transaction> findByDeletedFalseAndCategoryIgnoreCase(String category);

    List<Transaction> findByDeletedFalseAndDateBetween(LocalDate from, LocalDate to);

    List<Transaction> findByDeletedFalseAndTypeAndDateBetween(
            TransactionType type, LocalDate from, LocalDate to);

    List<Transaction> findByDeletedFalseAndCategoryIgnoreCaseAndDateBetween(
            String category, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.deleted = false AND t.type = :type")
    BigDecimal sumByType(@Param("type") TransactionType type);

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t WHERE t.deleted = false GROUP BY t.category ORDER BY SUM(t.amount) DESC")
    List<Object[]> sumGroupedByCategory();

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t WHERE t.deleted = false AND t.type = :type GROUP BY t.category ORDER BY SUM(t.amount) DESC")
    List<Object[]> sumGroupedByCategoryAndType(@Param("type") TransactionType type);

    @Query("SELECT FUNCTION('YEAR', t.date), FUNCTION('MONTH', t.date), t.type, SUM(t.amount) " +
           "FROM Transaction t WHERE t.deleted = false " +
           "GROUP BY FUNCTION('YEAR', t.date), FUNCTION('MONTH', t.date), t.type " +
           "ORDER BY FUNCTION('YEAR', t.date) DESC, FUNCTION('MONTH', t.date) DESC")
    List<Object[]> monthlyTrends();

    @Query("SELECT t FROM Transaction t WHERE t.deleted = false ORDER BY t.date DESC, t.createdAt DESC")
    List<Transaction> findRecentActivity();
}
