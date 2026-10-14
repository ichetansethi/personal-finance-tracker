package com.chetan.personalfinancetracker.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chetan.personalfinancetracker.model.Transaction;
import com.chetan.personalfinancetracker.model.User;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByUser(User user);
    @Query("SELECT t FROM Transaction t WHERE " +
       "t.user = :user AND " +
       "(:category IS NULL OR t.category.name = :category) AND " +
       "(:from IS NULL OR t.date >= :from) AND " +
       "(:to IS NULL OR t.date <= :to)")
    List<Transaction> filter(@Param("user") User user,
                            @Param("category") String category,
                            @Param("from") LocalDate from,
                            @Param("to") LocalDate to);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.category.type = 'INCOME' AND MONTH(t.date) = :month AND YEAR(t.date) = :year")
    Double getTotalIncomeForMonth(@Param("userId") Long userId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.category.type = 'EXPENSE' AND MONTH(t.date) = :month AND YEAR(t.date) = :year")
    Double getTotalExpenseForMonth(@Param("userId") Long userId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.category.type = 'INCOME' AND YEAR(t.date) = :year")
    Double getTotalIncomeForYear(@Param("userId") Long userId, @Param("year") int year);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.category.type = 'EXPENSE' AND YEAR(t.date) = :year")
    Double getTotalExpenseForYear(@Param("userId") Long userId, @Param("year") int year);

    @Query("SELECT t.category.id, t.category.name, SUM(t.amount) FROM Transaction t " +
        "WHERE t.user.id = :userId AND MONTH(t.date) = :month AND YEAR(t.date) = :year " +
        "GROUP BY t.category.id, t.category.name")
    List<Object[]> getCategoryWiseTotals(@Param("userId") Long userId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT t.category.id, t.category.name, SUM(t.amount) FROM Transaction t " +
        "WHERE t.user.id = :userId AND YEAR(t.date) = :year " +
        "GROUP BY t.category.id, t.category.name")
    List<Object[]> getCategoryWiseTotalsForYear(@Param("userId") Long userId, @Param("year") int year);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
        "WHERE t.user.id = :userId AND t.category.id = :categoryId " +
        "AND MONTH(t.date) = :month AND YEAR(t.date) = :year")
    Double getMonthlySpentForCategory(@Param("userId") Long userId,
                                    @Param("categoryId") Long categoryId,
                                    @Param("month") int month,
                                    @Param("year") int year);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
        "WHERE t.user.id = :userId AND t.category.id = :categoryId " +
        "AND YEAR(t.date) = :year")
    Double getYearlySpentForCategory(@Param("userId") Long userId,
                                    @Param("categoryId") Long categoryId,
                                    @Param("year") int year);

}
