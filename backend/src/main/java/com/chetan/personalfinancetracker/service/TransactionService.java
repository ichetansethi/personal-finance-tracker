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

        if (category.getUser() == null || !user.getId().equals(category.getUser().getId())) {
            throw new ResourceNotFoundException("Category not found");
        }

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

    // Filter transactions by category and/or date range for a specific user
    public List<TransactionDTO> filterTransactions(String username, String category, LocalDate from, LocalDate to) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Transaction> filtered = transactionRepository.filter(user, category, from, to);
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

    // Monthly summary for one user, including totals, per-category totals and budget alerts.
    public MonthlySummaryDTO getMonthlySummary(Long userId, int month, int year) {
        double income = zero(transactionRepository.getTotalIncomeForMonth(userId, month, year));
        double expense = zero(transactionRepository.getTotalExpenseForMonth(userId, month, year));
        return summarize(userId, income, expense, transactionRepository.getCategoryWiseTotals(userId, month, year), true, year);
    }

    // Yearly summary for one user. Totals cover the whole year and alerts use yearly budgets.
    public MonthlySummaryDTO getYearlySummary(Long userId, int year) {
        double income = zero(transactionRepository.getTotalIncomeForYear(userId, year));
        double expense = zero(transactionRepository.getTotalExpenseForYear(userId, year));
        return summarize(userId, income, expense, transactionRepository.getCategoryWiseTotalsForYear(userId, year), false, year);
    }

    private MonthlySummaryDTO summarize(Long userId, double income, double expense, List<Object[]> rows,
                                        boolean includeMonthlyAlerts, int year) {
        Map<Long, Category> categoriesById = new HashMap<>();
        for (Category category : categoryRepository.findByUserId(userId)) {
            categoriesById.put(category.getId(), category);
        }

        Map<String, Double> categorySummary = new HashMap<>();
        List<BudgetAlert> alerts = new ArrayList<>();

        for (Object[] row : rows) {
            Long categoryId = ((Number) row[0]).longValue();
            String categoryName = (String) row[1];
            double amount = toDouble(row[2]);
            categorySummary.put(summaryKey(categorySummary, categoryName, categoryId), amount);

            Category category = categoriesById.get(categoryId);
            if (category == null) {
                continue;
            }

            if (includeMonthlyAlerts && category.getMonthlyBudget() != null) {
                alerts.add(new BudgetAlert(
                    categoryName,
                    amount,
                    category.getMonthlyBudget(),
                    amount > category.getMonthlyBudget(),
                    "Monthly"
                ));
            }

            if (category.getYearlyBudget() != null) {
                double yearlySpent = includeMonthlyAlerts
                    ? zero(transactionRepository.getYearlySpentForCategory(userId, category.getId(), year))
                    : amount;
                alerts.add(new BudgetAlert(
                    categoryName,
                    yearlySpent,
                    category.getYearlyBudget(),
                    yearlySpent > category.getYearlyBudget(),
                    "Yearly"
                ));
            }
        }

        return new MonthlySummaryDTO(income, expense, categorySummary, alerts);
    }

    private static double zero(Double value) {
        return value == null ? 0.0 : value;
    }

    private static double toDouble(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.doubleValue();
        }
        return ((Number) value).doubleValue();
    }

    private static String summaryKey(Map<String, Double> summary, String name, Long id) {
        if (!summary.containsKey(name)) {
            return name;
        }
        return name + " (#" + id + ")";
    }

    // Budget alerts for a specific user, month, and year
    public List<BudgetAlert> getBudgetAlerts(Long userId, int month, int year) {
        List<Category> categories = categoryRepository.findByUserId(userId);
        List<BudgetAlert> alerts = new ArrayList<>();

        for (Category category : categories) {

            Double spentMonth = Optional.ofNullable(
                transactionRepository.getMonthlySpentForCategory(userId, category.getId(), month, year)
            ).orElse(0.0);

            Double spentYear = Optional.ofNullable(
                transactionRepository.getYearlySpentForCategory(userId, category.getId(), year)
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