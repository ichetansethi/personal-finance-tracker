package com.chetan.personalfinancetracker.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chetan.personalfinancetracker.dto.MonthlySummaryDTO;
import com.chetan.personalfinancetracker.dto.TransactionDTO;
import com.chetan.personalfinancetracker.exception.ResourceNotFoundException;
import com.chetan.personalfinancetracker.mapper.TransactionMapper;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Transaction;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.CategoryRepository;
import com.chetan.personalfinancetracker.repository.TransactionRepository;
import com.chetan.personalfinancetracker.repository.UserRepository;
import com.chetan.personalfinancetracker.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @PostMapping
    public Transaction addTransaction(@RequestBody TransactionDTO dto, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Category category = categoryRepository.findById(dto.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Transaction transaction = TransactionMapper.toEntity(dto, category, user);
        return transactionRepository.save(transaction);
    }

    @GetMapping
    public List<Transaction> getUserTransactions(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return transactionRepository.findByUser(user);
    }

    @GetMapping("/filter")
    public List<TransactionDTO> getFilteredTransactions(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) 
        {
        return transactionService.filterTransactions(category, from, to);
    }

    @GetMapping("/summary")
    public ResponseEntity<MonthlySummaryDTO> getMonthlySummary(
            @RequestParam int month,
            @RequestParam int year) {
        MonthlySummaryDTO summary = transactionService.getMonthlySummary(month, year);
        return ResponseEntity.ok(summary);
    }
    
}
