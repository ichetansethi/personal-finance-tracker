package com.chetan.personalfinancetracker.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.chetan.personalfinancetracker.TransactionType;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Expense;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.support.TestEntities;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExpenseRepositoryTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void expensesAreIsolatedByUserId() {
        User alice = userRepository.saveAndFlush(TestEntities.user("alice"));
        User bob = userRepository.saveAndFlush(TestEntities.user("bob"));
        Category food = categoryRepository.saveAndFlush(TestEntities.category("Food", TransactionType.EXPENSE, alice, 100.0, null));
        expenseRepository.saveAndFlush(expense(alice, food, "12.00", "Lunch"));
        expenseRepository.saveAndFlush(expense(bob, food, "40.00", "Taxi"));

        assertThat(expenseRepository.findByUserId(alice.getId())).extracting(Expense::getDescription)
                .containsExactly("Lunch");
        assertThat(expenseRepository.findByUserId(bob.getId())).extracting(Expense::getDescription)
                .containsExactly("Taxi");
    }

    private static Expense expense(User user, Category category, String amount, String description) {
        Expense expense = new Expense();
        expense.setUser(user);
        expense.setCategory(category);
        expense.setAmount(new BigDecimal(amount));
        expense.setDescription(description);
        expense.setDate(LocalDate.of(2026, 3, 2));
        return expense;
    }
}
