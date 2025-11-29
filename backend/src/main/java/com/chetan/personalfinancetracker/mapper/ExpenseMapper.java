package com.chetan.personalfinancetracker.mapper;

import com.chetan.personalfinancetracker.dto.ExpenseDTO;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Expense;

public class ExpenseMapper {

    public static ExpenseDTO toDTO(Expense expense) {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setId(expense.getId());
        dto.setDescription(expense.getDescription());
        dto.setAmount(expense.getAmount());
        dto.setDate(expense.getDate());
        dto.setCategoryId(expense.getId());
        dto.setCategoryName(expense.getCategory().getName());
        return dto;
    }

    public static Expense toEntity(ExpenseDTO dto, Category category) {
        Expense expense = new Expense();
        expense.setId(dto.getId());
        expense.setDescription(dto.getDescription());
        expense.setAmount(dto.getAmount());
        expense.setDate(dto.getDate());
        expense.setCategory(expense.getCategory());
        return expense;
    }
}
