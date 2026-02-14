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

import jakarta.validation.Valid;

import com.chetan.personalfinancetracker.dto.IncomeDTO;
import com.chetan.personalfinancetracker.exception.ResourceNotFoundException;
import com.chetan.personalfinancetracker.mapper.IncomeMapper;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Income;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.IncomeRepository;
import com.chetan.personalfinancetracker.repository.UserRepository;
import com.chetan.personalfinancetracker.service.CategoryService;
import com.chetan.personalfinancetracker.service.IncomeService;

@RestController
@RequestMapping("/api/incomes")
public class IncomeController {

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private IncomeService incomeService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user")
    public List<Income> getMyIncomes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return incomeRepository.findByUserId(user.getId());
    }

    @PostMapping
    public ResponseEntity<IncomeDTO> createIncome(@Valid @RequestBody IncomeDTO dto) {
        Category category = categoryService.findById(dto.getCategoryId());
        Income income = IncomeMapper.toEntity(dto, category);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        income.setUser(user);

        Income saved = incomeService.createIncome(income);
        return ResponseEntity.ok(IncomeMapper.toDTO(saved));
    }

}
