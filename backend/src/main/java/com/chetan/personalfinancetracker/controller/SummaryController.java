package com.chetan.personalfinancetracker.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chetan.personalfinancetracker.dto.MonthlySummaryDTO;
import com.chetan.personalfinancetracker.dto.MonthlySummaryDTO.BudgetAlert;
import com.chetan.personalfinancetracker.exception.ResourceNotFoundException;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.UserRepository;
import com.chetan.personalfinancetracker.service.TransactionService;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/monthly")
    public ResponseEntity<MonthlySummaryDTO> getMonthlySummary(
            @RequestParam int month,
            @RequestParam int year) {

        MonthlySummaryDTO summary = transactionService.getMonthlySummary(currentUser().getId(), month, year);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/yearly")
    public ResponseEntity<MonthlySummaryDTO> getYearlySummary(
            @RequestParam int year) {

        MonthlySummaryDTO summary = transactionService.getYearlySummary(currentUser().getId(), year);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/budget-alerts")
    public ResponseEntity<List<BudgetAlert>> getBudgetAlerts(
            @RequestParam int month,
            @RequestParam int year) {

        List<BudgetAlert> alerts = transactionService.getBudgetAlerts(currentUser().getId(), month, year);
        return ResponseEntity.ok(alerts);
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}