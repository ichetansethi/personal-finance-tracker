package com.chetan.personalfinancetracker.dto;

public class CategoryDTO {
    private Long id;
    private String name;
    private String type;

    public CategoryDTO() {}

    public CategoryDTO(Long id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    private Long userId;

    private Double budgetLimit;

    private Double monthlyBudget;
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
