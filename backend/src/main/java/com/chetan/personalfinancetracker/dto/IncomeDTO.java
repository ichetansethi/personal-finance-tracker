package com.chetan.personalfinancetracker.dto;

import java.time.LocalDate;

import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Income;

public class IncomeDTO {

    private Long id;
    private String source;
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
    public String getSource() {
        return source;
    }
    public void setSource(String source) {
        this.source = source;
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

    public static IncomeDTO toDTO(Income income, Category category) {
        IncomeDTO dto = new IncomeDTO();
        dto.setId(income.getId());
        dto.setSource(income.getSource());
        dto.setAmount(income.getAmount());
        dto.setDate(income.getDate());
        dto.setCategoryId(income.getCategory() != null ? category.getId() : null);
        dto.setCategoryName(income.getCategory());
        return dto;
    }

    public static Income toEntity(IncomeDTO dto, Category category) {
        Income income = new Income();
        income.setId(dto.getId());
        income.setSource(dto.getSource());
        income.setAmount(dto.getAmount());
        income.setDate(dto.getDate());
        income.setCategory(category != null ? category.getName() : null);
        return income;
    }

}
