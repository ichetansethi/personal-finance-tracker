package com.chetan.personalfinancetracker.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import com.chetan.personalfinancetracker.TransactionType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "budget_limit")
    private Double budgetLimit;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @ManyToOne
    @JoinColumn(name = "user_id") // this should point to a user_id column in your DB
    private User user;

    @Column(name = "monthly_budget")
    private Double monthlyBudget;

    @Column(name = "yearly_budget")
    private Double yearlyBudget;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @JsonManagedReference("category-transaction")
    private List<Transaction> transactions=new ArrayList<>();

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    // Constructors
    public Category() {}

    public Category(String name, TransactionType type, User user) {
        this.name = name;
        this.type = type;
        this.user = user;
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TransactionType getType() {
        return type;
    }

    public User getUser() {
        return user;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public void setUser(User user) {
        this.user = user;
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
