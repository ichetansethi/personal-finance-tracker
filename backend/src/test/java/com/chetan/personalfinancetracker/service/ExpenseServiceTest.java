package com.chetan.personalfinancetracker.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chetan.personalfinancetracker.model.Expense;
import com.chetan.personalfinancetracker.repository.ExpenseRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseService expenseService;

    @Test
    void createExpenseSavesWithoutEnforcingABudgetCeiling() {
        Expense expense = new Expense();
        expense.setAmount(new BigDecimal("9999.00"));
        when(expenseRepository.save(expense)).thenReturn(expense);

        assertThat(expenseService.createExpense(expense)).isSameAs(expense);
    }

    @Test
    void getAllExpensesReturnsTheRepositoryContents() {
        when(expenseRepository.findAll()).thenReturn(List.of(new Expense(), new Expense()));

        assertThat(expenseService.getAllExpenses()).hasSize(2);
    }
}
