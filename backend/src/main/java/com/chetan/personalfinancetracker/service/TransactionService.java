package com.chetan.personalfinancetracker.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chetan.personalfinancetracker.dto.MonthlySummaryDTO;
import com.chetan.personalfinancetracker.dto.MonthlySummaryDTO.BudgetAlert;
import com.chetan.personalfinancetracker.dto.TransactionDTO;
import com.chetan.personalfinancetracker.exception.ResourceNotFoundException;
import com.chetan.personalfinancetracker.mapper.TransactionMapper;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Transaction;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.CategoryRepository;
import com.chetan.personalfinancetracker.repository.TransactionRepository;
import com.chetan.personalfinancetracker.repository.UserRepository;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    // Create a new transaction
    public TransactionDTO createTransaction(TransactionDTO dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        User user = userRepository.findById(dto.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Transaction transaction = TransactionMapper.toEntity(dto, category, user);
        Transaction saved = transactionRepository.save(transaction);

        return TransactionMapper.toDTO(saved);
    }

    // Get all transactions
    public List<TransactionDTO> getAllTransactions() {
        return transactionRepository.findAll()
                .stream()
                .map(TransactionMapper::toDTO)
                .toList();
    }

    // Filter transactions by category and/or date range
    public List<TransactionDTO> filterTransactions(String category, LocalDate from, LocalDate to) {
        List<Transaction> filtered = transactionRepository.filter(category, from, to);
        return filtered.stream()
                .map(TransactionMapper::toDTO)
                .toList();
    }

    // Delete transaction
    public void deleteTransaction(Long id) {
        transactionRepository.deleteById(id);
    }

    // Update an existing transaction
    public TransactionDTO updateTransaction(Long id, TransactionDTO dto) {
        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        existing.setAmount(dto.getAmount());
        existing.setDescription(dto.getDescription());
        existing.setDate(dto.getDate());
        // If you want to allow changing category/user, handle that here too.

        Transaction updated = transactionRepository.save(existing);
        return TransactionMapper.toDTO(updated);
    }

    // Monthly summary including totals, category-wise totals and budget alerts
    public MonthlySummaryDTO getMonthlySummary(int month, int year) {
        Double income  = Optional.ofNullable(transactionRepository.getTotalIncomeForMonth(month, year)).orElse(0.0);
        Double expense = Optional.ofNullable(transactionRepository.getTotalExpenseForMonth(month, year)).orElse(0.0);

        List<Object[]> categoryData = transactionRepository.getCategoryWiseTotals(month, year);
        Map<String, Double> categorySummary = new HashMap<>();
        List<BudgetAlert> alerts = new ArrayList<>();

        // Build lookup map: category name -> Category entity
        List<Category> allCategories = categoryRepository.findAll();
        Map<String, Category> categoryMap = new HashMap<>();
        for (Category cat : allCategories) {
            categoryMap.put(cat.getName(), cat);
        }

        for (Object[] row : categoryData) {
            String categoryName = (String) row[0];

            // row[1] might be BigDecimal or some Number depending on JPA provider
            Double amount;
            if (row[1] instanceof BigDecimal bigDecimal) {
                amount = bigDecimal.doubleValue();
            } else {
                amount = ((Number) row[1]).doubleValue();
            }

            categorySummary.put(categoryName, amount);

            Category category = categoryMap.get(categoryName);
            if (category == null) {
                // No matching Category entity (shouldn't normally happen), skip alerts for this one
                continue;
            }

            // Monthly budget alert
            if (category.getMonthlyBudget() != null) {
                boolean exceededMonthly = amount > category.getMonthlyBudget();
                alerts.add(new BudgetAlert(
                    categoryName,
                    amount,
                    category.getMonthlyBudget(),
                    exceededMonthly,
                    "Monthly"
                ));
            }

            // Yearly budget alert
            if (category.getYearlyBudget() != null) {
                Double yearlySpent = Optional.ofNullable(
                        transactionRepository.getYearlySpentForCategory(category.getId(), year)
                ).orElse(0.0);

                boolean exceededYearly = yearlySpent > category.getYearlyBudget();
                alerts.add(new BudgetAlert(
                    categoryName,
                    yearlySpent,
                    category.getYearlyBudget(),
                    exceededYearly,
                    "Yearly"
                ));
            }
        }

        return new MonthlySummaryDTO(income, expense, categorySummary, alerts);
    }

    // Budget alerts for a specific user, month, and year
    public List<BudgetAlert> getBudgetAlerts(Long userId, int month, int year) {
        List<Category> categories = categoryRepository.findByUserId(userId);
        List<BudgetAlert> alerts = new ArrayList<>();

        for (Category category : categories) {

            Double spentMonth = Optional.ofNullable(
                transactionRepository.getMonthlySpentForCategory(category.getId(), month, year)
            ).orElse(0.0);

            Double spentYear = Optional.ofNullable(
                transactionRepository.getYearlySpentForCategory(category.getId(), year)
            ).orElse(0.0);

            if (category.getMonthlyBudget() != null) {
                alerts.add(new BudgetAlert(
                    category.getName(),
                    spentMonth,
                    category.getMonthlyBudget(),
                    spentMonth > category.getMonthlyBudget(),
                    "Monthly"
                ));
            }

            if (category.getYearlyBudget() != null) {
                alerts.add(new BudgetAlert(
                    category.getName(),
                    spentYear,
                    category.getYearlyBudget(),
                    spentYear > category.getYearlyBudget(),
                    "Yearly"
                ));
            }
        }

        return alerts;
    }
}