package com.chetan.personalfinancetracker.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chetan.personalfinancetracker.dto.ExpenseDTO;
import com.chetan.personalfinancetracker.exception.ResourceNotFoundException;
import com.chetan.personalfinancetracker.mapper.ExpenseMapper;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Expense;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.ExpenseRepository;
import com.chetan.personalfinancetracker.repository.UserRepository;
import com.chetan.personalfinancetracker.service.CategoryService;
import com.chetan.personalfinancetracker.service.ExpenseService;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/my")
    public List<Expense> getMyExpenses() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return expenseRepository.findByUserId(user.getId());
    }

    @PostMapping
    public ResponseEntity<ExpenseDTO> createExpense(@RequestBody ExpenseDTO dto) {
        Category category = categoryService.findById(dto.getCategoryId());
        Expense expense = ExpenseMapper.toEntity(dto, category);
        Expense saved = expenseService.createExpense(expense);
        return ResponseEntity.ok(ExpenseMapper.toDTO(saved));
    }

}
