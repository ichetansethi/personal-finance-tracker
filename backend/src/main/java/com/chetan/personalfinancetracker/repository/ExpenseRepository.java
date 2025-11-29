package com.chetan.personalfinancetracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chetan.personalfinancetracker.model.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserId(Long userId);
}
