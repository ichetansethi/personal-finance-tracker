package com.chetan.personalfinancetracker.support;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.chetan.personalfinancetracker.TransactionType;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Transaction;
import com.chetan.personalfinancetracker.model.User;

public final class TestEntities {

    private TestEntities() {
    }

    public static User user(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("hashed-password");
        return user;
    }

    public static Category category(String name, TransactionType type, User user, Double monthly, Double yearly) {
        Category category = new Category(name, type, user);
        category.setMonthlyBudget(monthly);
        category.setYearlyBudget(yearly);
        return category;
    }

    public static Transaction transaction(User user, Category category, String amount, LocalDate date, String description) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDate(date);
        transaction.setDescription(description);
        transaction.setType(category.getType());
        return transaction;
    }
}
