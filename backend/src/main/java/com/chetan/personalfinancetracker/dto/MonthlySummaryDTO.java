package com.chetan.personalfinancetracker.dto;

import java.util.List;
import java.util.Map;

public class MonthlySummaryDTO {
    private double totalIncome;
    private double totalExpense;
    private double netBalance;
    private Map<String, Double> categorySummary;
    private List<BudgetAlert> budgetAlerts;

    public MonthlySummaryDTO(double totalIncome, double totalExpense, Map<String, Double>categorySummary, List<BudgetAlert> budgetAlerts) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.netBalance = totalIncome - totalExpense;
        this.categorySummary = categorySummary;
        this.budgetAlerts = budgetAlerts;
    }

    // Getters & Setters
    public double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

    public double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(double totalExpense) { this.totalExpense = totalExpense; }

    public double getNetBalance() { return netBalance; }
    public void setNetBalance(double netBalance) { this.netBalance = netBalance; }

    public Map<String, Double> getCategorySummary() {
        return categorySummary;
    }

    public void setCategorySummary(Map<String, Double> categorySummary) {
        this.categorySummary = categorySummary;
    }

    public List<BudgetAlert> getBudgetAlerts() {
        return budgetAlerts;
    }

    public void setBudgetAlerts(List<BudgetAlert> budgetAlerts) {
        this.budgetAlerts = budgetAlerts;
    }

    public static class BudgetAlert {
        private final String category;
        private final Double spent;
        private final Double budget;
        private final boolean exceeded;
        private final String period;

        public BudgetAlert(String category, Double spent, Double budget, boolean exceeded, String period) {
            this.category = category;
            this.spent = spent;
            this.budget = budget;
            this.exceeded = exceeded;
            this.period = period;
        }

        public String getCategory() { return category; }
        public Double getSpent() { return spent; }
        public Double getBudget() { return budget; }
        public boolean isExceeded() { return exceeded; }
        public String getPeriod() { return period; }
    }

}