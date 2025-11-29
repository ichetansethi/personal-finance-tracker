package com.chetan.personalfinancetracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chetan.personalfinancetracker.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Category findByName(String categoryName);

    // Fetch total expenses per category per month/year
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.category.id = :categoryId AND MONTH(t.date) = :month AND YEAR(t.date) = :year")
    Double getMonthlySpentForCategory(@Param("categoryId") Long categoryId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.category.id = :categoryId AND YEAR(t.date) = :year")
    Double getYearlySpentForCategory(@Param("categoryId") Long categoryId, @Param("year") int year);

    List<Category> findByUserId(Long userId);

}
