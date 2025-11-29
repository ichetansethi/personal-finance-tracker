package com.chetan.personalfinancetracker.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chetan.personalfinancetracker.dto.IncomeDTO;
import com.chetan.personalfinancetracker.mapper.IncomeMapper;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Income;
import com.chetan.personalfinancetracker.repository.IncomeRepository;
import com.chetan.personalfinancetracker.service.CategoryService;
import com.chetan.personalfinancetracker.service.IncomeService;

@RestController
@RequestMapping("/api/incomes")
public class IncomeController {

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/user/{userId}")
    public List<Income> getIncomesForUser(@PathVariable Long userId) {
        return incomeRepository.findByUserId(userId);
    }

    @Autowired
    private IncomeService incomeService;

    @PostMapping
    public ResponseEntity<IncomeDTO> createIncome(@RequestBody IncomeDTO dto) {
        Category category = categoryService.findById(dto.getCategoryId());
        Income income = IncomeMapper.toEntity(dto, category);
        Income saved = incomeService.createIncome(income);
        return ResponseEntity.ok(IncomeMapper.toDTO(saved));
    }

}
