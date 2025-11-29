package com.chetan.personalfinancetracker.model;

public class BudgetAlerts {
    private String category;
    private double spent;
    private double budgetLimit;
    private boolean exceeded;
    private String period; // "Monthly" or "Yearly"

    public BudgetAlerts(String category, double spent, double budgetLimit, boolean exceeded, String period) {
        this.category = category;
        this.spent = spent;
        this.budgetLimit = budgetLimit;
        this.exceeded = exceeded;
        this.period = period;
    }

    public String getCategory() {
        return category;
    }

    public double getSpent() {
        return spent;
    }

    public double getBudgetLimit() {
        return budgetLimit;
    }

    public boolean isExceeded() {
        return exceeded;
    }

    public String getPeriod() {
        return period;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }

    public void setSpent(double spent) {
        this.spent = spent;
    }

    public void setBudgetLimit(double budgetLimit) {
        this.budgetLimit = budgetLimit;     
    }

    public void setExceeded(boolean exceeded) {
        this.exceeded = exceeded;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

}
