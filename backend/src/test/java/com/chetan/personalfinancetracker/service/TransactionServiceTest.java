package com.chetan.personalfinancetracker.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chetan.personalfinancetracker.TransactionType;
import com.chetan.personalfinancetracker.dto.MonthlySummaryDTO;
import com.chetan.personalfinancetracker.dto.MonthlySummaryDTO.BudgetAlert;
import com.chetan.personalfinancetracker.dto.TransactionDTO;
import com.chetan.personalfinancetracker.exception.ResourceNotFoundException;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Transaction;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.CategoryRepository;
import com.chetan.personalfinancetracker.repository.TransactionRepository;
import com.chetan.personalfinancetracker.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void createPersistsATransactionForTheRequestedUserAndCategory() {
        TransactionDTO dto = dto(new BigDecimal("42.00"), TransactionType.EXPENSE, LocalDate.of(2026, 1, 5), "Coffee", 3L, 8L);
        Category category = category(3L, "Food", 200.0, 2000.0);
        User user = user(8L, "alice");
        category.setUser(user);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(userRepository.findById(8L)).thenReturn(Optional.of(user));
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(15L);
            return transaction;
        });

        TransactionDTO saved = transactionService.createTransaction(dto);

        assertThat(saved.getId()).isEqualTo(15L);
        assertThat(saved.getAmount()).isEqualByComparingTo("42.00");
        assertThat(saved.getCategoryId()).isEqualTo(3L);
        assertThat(saved.getCategoryName()).isEqualTo("Food");
        assertThat(saved.getUserId()).isEqualTo(8L);
        assertThat(saved.getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void createRejectsAnUnknownCategory() {
        when(categoryRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(
                dto(BigDecimal.TEN, TransactionType.EXPENSE, LocalDate.now(), "Coffee", 3L, 8L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createRejectsAnUnknownUser() {
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category(3L, "Food", null, null)));
        when(userRepository.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(
                dto(BigDecimal.TEN, TransactionType.EXPENSE, LocalDate.now(), "Coffee", 3L, 8L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createRejectsACategoryOwnedBySomeoneElse() {
        User alice = user(8L, "alice");
        User bob = user(9L, "bob");
        Category category = category(3L, "Food", null, null);
        category.setUser(bob);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(userRepository.findById(8L)).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> transactionService.createTransaction(
                dto(BigDecimal.TEN, TransactionType.EXPENSE, LocalDate.now(), "Coffee", 3L, 8L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void filterReturnsOnlyTheNamedUsersMatches() {
        User alice = user(1L, "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        Transaction lunch = stored(4L, alice, category(3L, "Food", null, null), "12.00", LocalDate.of(2026, 3, 2), "Lunch");
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        when(transactionRepository.filter(alice, "Food", from, to)).thenReturn(List.of(lunch));

        List<TransactionDTO> results = transactionService.filterTransactions("alice", "Food", from, to);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDescription()).isEqualTo("Lunch");
        assertThat(results.get(0).getUserId()).isEqualTo(1L);
        verify(transactionRepository).filter(alice, "Food", from, to);
    }

    @Test
    void filterRejectsAnUnknownUser() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.filterTransactions("missing", null, null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(transactionRepository, never()).filter(any(), any(), any(), any());
    }

    @Test
    void updateChangesAmountDescriptionAndDateOnly() {
        Category salary = category(2L, "Salary", null, null);
        User alice = user(4L, "alice");
        Transaction existing = stored(1L, alice, salary, "5.00", LocalDate.of(2026, 1, 1), "old");
        existing.setType(TransactionType.INCOME);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionDTO changes = dto(new BigDecimal("9.00"), TransactionType.EXPENSE, LocalDate.of(2026, 2, 2), "new", 99L, 123L);
        TransactionDTO updated = transactionService.updateTransaction(1L, changes);

        assertThat(updated.getAmount()).isEqualByComparingTo("9.00");
        assertThat(updated.getDescription()).isEqualTo("new");
        assertThat(updated.getDate()).isEqualTo(LocalDate.of(2026, 2, 2));
        assertThat(updated.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(updated.getCategoryId()).isEqualTo(2L);
        assertThat(updated.getUserId()).isEqualTo(4L);
    }

    @Test
    void updateRejectsAnUnknownTransaction() {
        when(transactionRepository.findById(44L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateTransaction(44L,
                dto(BigDecimal.ONE, TransactionType.EXPENSE, LocalDate.now(), "x", 1L, 1L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transaction not found");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deleteDelegatesToTheRepository() {
        transactionService.deleteTransaction(5L);

        verify(transactionRepository).deleteById(5L);
    }

    @Test
    void monthlySummaryTreatsMissingTotalsAsZero() {
        when(transactionRepository.getTotalIncomeForMonth(4L, 1, 2026)).thenReturn(null);
        when(transactionRepository.getTotalExpenseForMonth(4L, 1, 2026)).thenReturn(null);
        when(transactionRepository.getCategoryWiseTotals(4L, 1, 2026)).thenReturn(List.of());
        when(categoryRepository.findByUserId(4L)).thenReturn(List.of());

        MonthlySummaryDTO summary = transactionService.getMonthlySummary(4L, 1, 2026);

        assertThat(summary.getTotalIncome()).isZero();
        assertThat(summary.getTotalExpense()).isZero();
        assertThat(summary.getNetBalance()).isZero();
        assertThat(summary.getCategorySummary()).isEmpty();
        assertThat(summary.getBudgetAlerts()).isEmpty();
    }

    @Test
    void monthlySummaryFlagsBudgetsThatAreStrictlyExceeded() {
        when(transactionRepository.getTotalIncomeForMonth(4L, 3, 2026)).thenReturn(1000.0);
        when(transactionRepository.getTotalExpenseForMonth(4L, 3, 2026)).thenReturn(400.0);

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] {1L, "Food", new BigDecimal("250.50")});
        rows.add(new Object[] {2L, "Rent", 100});
        rows.add(new Object[] {99L, "Ghost", 15});
        when(transactionRepository.getCategoryWiseTotals(4L, 3, 2026)).thenReturn(rows);

        Category food = category(1L, "Food", 200.0, 250.50);
        Category rent = category(2L, "Rent", 100.0, null);
        when(categoryRepository.findByUserId(4L)).thenReturn(List.of(food, rent));
        when(transactionRepository.getYearlySpentForCategory(4L, 1L, 2026)).thenReturn(null);

        MonthlySummaryDTO summary = transactionService.getMonthlySummary(4L, 3, 2026);

        assertThat(summary.getTotalIncome()).isEqualTo(1000.0);
        assertThat(summary.getTotalExpense()).isEqualTo(400.0);
        assertThat(summary.getNetBalance()).isEqualTo(600.0);
        assertThat(summary.getCategorySummary())
                .containsEntry("Food", 250.50)
                .containsEntry("Rent", 100.0)
                .containsEntry("Ghost", 15.0);

        assertThat(summary.getBudgetAlerts()).extracting(BudgetAlert::getCategory, BudgetAlert::getPeriod, BudgetAlert::isExceeded)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Food", "Monthly", true),
                        org.assertj.core.groups.Tuple.tuple("Food", "Yearly", false),
                        org.assertj.core.groups.Tuple.tuple("Rent", "Monthly", false));
        BudgetAlert foodYear = summary.getBudgetAlerts().get(1);
        assertThat(foodYear.getSpent()).isZero();
        assertThat(foodYear.getBudget()).isEqualTo(250.50);
    }

    @Test
    void budgetAlertsAreComputedPerUserAndTreatMissingSpendAsZero() {
        Category groceries = category(7L, "Groceries", 100.0, 1000.0);
        Category savings = category(8L, "Savings", null, 500.0);
        Category unbudgeted = category(9L, "Gifts", null, null);
        when(categoryRepository.findByUserId(4L)).thenReturn(List.of(groceries, savings, unbudgeted));
        when(transactionRepository.getMonthlySpentForCategory(4L, 7L, 3, 2026)).thenReturn(100.0);
        when(transactionRepository.getYearlySpentForCategory(4L, 7L, 2026)).thenReturn(1000.01);
        when(transactionRepository.getYearlySpentForCategory(4L, 8L, 2026)).thenReturn(null);

        List<BudgetAlert> alerts = transactionService.getBudgetAlerts(4L, 3, 2026);

        assertThat(alerts).extracting(BudgetAlert::getCategory, BudgetAlert::getPeriod, BudgetAlert::isExceeded, BudgetAlert::getSpent)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Groceries", "Monthly", false, 100.0),
                        org.assertj.core.groups.Tuple.tuple("Groceries", "Yearly", true, 1000.01),
                        org.assertj.core.groups.Tuple.tuple("Savings", "Yearly", false, 0.0));
    }

    @Test
    void summaryDoesNotMergeCategoriesThatShareAName() {
        Category groceries = category(1L, "Food", 50.0, null);
        Category dining = category(2L, "Food", 10.0, 20.0);
        when(categoryRepository.findByUserId(4L)).thenReturn(List.of(groceries, dining));
        when(transactionRepository.getTotalIncomeForMonth(4L, 3, 2026)).thenReturn(0.0);
        when(transactionRepository.getTotalExpenseForMonth(4L, 3, 2026)).thenReturn(70.0);
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] {1L, "Food", 40.0});
        rows.add(new Object[] {2L, "Food", 30.0});
        when(transactionRepository.getCategoryWiseTotals(4L, 3, 2026)).thenReturn(rows);
        when(transactionRepository.getYearlySpentForCategory(4L, 2L, 2026)).thenReturn(30.0);

        MonthlySummaryDTO summary = transactionService.getMonthlySummary(4L, 3, 2026);

        assertThat(summary.getCategorySummary()).containsEntry("Food", 40.0).containsEntry("Food (#2)", 30.0);
        assertThat(summary.getBudgetAlerts()).extracting(BudgetAlert::getCategory, BudgetAlert::getPeriod, BudgetAlert::isExceeded, BudgetAlert::getSpent)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Food", "Monthly", false, 40.0),
                        org.assertj.core.groups.Tuple.tuple("Food", "Monthly", true, 30.0),
                        org.assertj.core.groups.Tuple.tuple("Food", "Yearly", true, 30.0));
    }

    @Test
    void yearlySummaryUsesTheWholeYearAndOnlyYearlyBudgets() {
        Category rent = category(5L, "Rent", 100.0, 1000.0);
        when(categoryRepository.findByUserId(4L)).thenReturn(List.of(rent));
        when(transactionRepository.getTotalIncomeForYear(4L, 2026)).thenReturn(5000.0);
        when(transactionRepository.getTotalExpenseForYear(4L, 2026)).thenReturn(1200.0);
        List<Object[]> yearRows = new ArrayList<>();
        yearRows.add(new Object[] {5L, "Rent", 1200.0});
        when(transactionRepository.getCategoryWiseTotalsForYear(4L, 2026)).thenReturn(yearRows);

        MonthlySummaryDTO summary = transactionService.getYearlySummary(4L, 2026);

        assertThat(summary.getTotalExpense()).isEqualTo(1200.0);
        assertThat(summary.getNetBalance()).isEqualTo(3800.0);
        assertThat(summary.getBudgetAlerts()).extracting(BudgetAlert::getPeriod, BudgetAlert::isExceeded, BudgetAlert::getSpent)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Yearly", true, 1200.0));
        verify(transactionRepository, never()).getTotalExpenseForMonth(any(), any(int.class), any(int.class));
    }

    @Test
    void budgetAlertsAreEmptyWhenTheUserHasNoCategories() {
        when(categoryRepository.findByUserId(4L)).thenReturn(List.of());

        assertThat(transactionService.getBudgetAlerts(4L, 3, 2026)).isEmpty();
    }

    private static TransactionDTO dto(BigDecimal amount, TransactionType type, LocalDate date, String description,
                                      Long categoryId, Long userId) {
        TransactionDTO dto = new TransactionDTO();
        dto.setAmount(amount);
        dto.setType(type);
        dto.setDate(date);
        dto.setDescription(description);
        dto.setCategoryId(categoryId);
        dto.setUserId(userId);
        return dto;
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static Category category(Long id, String name, Double monthly, Double yearly) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setMonthlyBudget(monthly);
        category.setYearlyBudget(yearly);
        return category;
    }

    private static Transaction stored(Long id, User user, Category category, String amount, LocalDate date, String description) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDate(date);
        transaction.setDescription(description);
        transaction.setType(TransactionType.EXPENSE);
        return transaction;
    }
}
