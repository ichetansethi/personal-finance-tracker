package com.chetan.personalfinancetracker.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chetan.personalfinancetracker.dto.MonthlySummaryDTO;
import com.chetan.personalfinancetracker.dto.MonthlySummaryDTO.BudgetAlert;
import com.chetan.personalfinancetracker.service.TransactionService;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    @Autowired
    private TransactionService transactionService;

    // -------------------------------------
    // 1️⃣ Monthly Summary
    // -------------------------------------
    @GetMapping("/monthly")
    public ResponseEntity<MonthlySummaryDTO> getMonthlySummary(
            @RequestParam int month,
            @RequestParam int year) {

        MonthlySummaryDTO summary = transactionService.getMonthlySummary(month, year);
        return ResponseEntity.ok(summary);
    }

    // -------------------------------------
    // 2️⃣ Yearly Summary (Optional but useful)
    // -------------------------------------
    @GetMapping("/yearly")
    public ResponseEntity<MonthlySummaryDTO> getYearlySummary(
            @RequestParam int year) {

        // Reuse monthly summary logic:
        // month = 0 → service internally handles "year only"
        MonthlySummaryDTO summary = transactionService.getMonthlySummary(0, year);
        return ResponseEntity.ok(summary);
    }

    // -------------------------------------
    // 3️⃣ Budget Alerts Endpoint
    // -------------------------------------
    @GetMapping("/budget-alerts")
    public ResponseEntity<List<BudgetAlert>> getBudgetAlerts(
            @RequestParam Long userId,
            @RequestParam int month,
            @RequestParam int year) {

        List<BudgetAlert> alerts = transactionService.getBudgetAlerts(userId, month, year);
        return ResponseEntity.ok(alerts);
    }
}