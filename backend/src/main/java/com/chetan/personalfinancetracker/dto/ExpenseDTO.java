package com.chetan.personalfinancetracker.dto;

import java.time.LocalDate;

import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Expense;

public class ExpenseDTO {
    private Long id;
    private String description;
    private Double amount;
    private LocalDate date;
    private Long categoryId;
    private String categoryName;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Double getAmount() {
        return amount;
    }
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    public LocalDate getDate() {
        return date;
    }
    public void setDate(LocalDate date) {
        this.date = date;
    }
    public Long getCategoryId() {
        return categoryId;
    }
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    public String getCategoryName() {
        return categoryName;
    }
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public static ExpenseDTO toDTO(Expense expense) {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setId(expense.getId());
        dto.setDescription(expense.getDescription());
        dto.setAmount(expense.getAmount());
        dto.setDate(expense.getDate());
        dto.setCategoryId(expense.getCategory() != null ? expense.getCategory().getId() : null);
        dto.setCategoryName(expense.getCategory() != null ? expense.getCategory().getName() : null);
        return dto;
    }

    public static Expense toEntity(ExpenseDTO dto, Category category) {
        Expense expense = new Expense();
        expense.setId(dto.getId());
        expense.setDescription(dto.getDescription());
        expense.setAmount(dto.getAmount());
        expense.setDate(dto.getDate());
        expense.setCategory(category);
        return expense;
    }

}
