package com.chetan.personalfinancetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public class CategoryDTO {
    private Long id;

    @NotBlank(message = "Category name is required")
    private String name;

    @NotBlank(message = "Category type is required")
    private String type;

    public CategoryDTO() {}

    public CategoryDTO(Long id, String name, Object type) {
        this.id = id;
        this.name = name;
        this.type = type != null ? type.toString() : null;
    }

    private Long userId;

    private Double budgetLimit;

    @PositiveOrZero(message = "Monthly budget must be zero or positive")
    private Double monthlyBudget;

    @PositiveOrZero(message = "Yearly budget must be zero or positive")
    private Double yearlyBudget;

    // Getters and Setters

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getBudgetLimit() {
        return budgetLimit;
    }

    public void setBudgetLimit(Double budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    public Double getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(Double monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public Double getYearlyBudget() {
        return yearlyBudget;
    }

    public void setYearlyBudget(Double yearlyBudget) {
        this.yearlyBudget = yearlyBudget;
    }

}
