package com.chetan.personalfinancetracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chetan.personalfinancetracker.model.Income;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findByUserId(Long userId);
}
