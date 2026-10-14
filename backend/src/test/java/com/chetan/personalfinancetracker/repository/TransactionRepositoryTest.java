package com.chetan.personalfinancetracker.repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.chetan.personalfinancetracker.TransactionType;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Transaction;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.support.TestEntities;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User alice;
    private User bob;
    private Category aliceFood;
    private Category aliceSalary;
    private Category bobFood;

    @BeforeEach
    void persistLedger() {
        alice = userRepository.saveAndFlush(TestEntities.user("alice"));
        bob = userRepository.saveAndFlush(TestEntities.user("bob"));
        aliceFood = categoryRepository.saveAndFlush(TestEntities.category("Food", TransactionType.EXPENSE, alice, 100.0, 1000.0));
        aliceSalary = categoryRepository.saveAndFlush(TestEntities.category("Salary", TransactionType.INCOME, alice, null, null));
        bobFood = categoryRepository.saveAndFlush(TestEntities.category("Food", TransactionType.EXPENSE, bob, 50.0, 500.0));

        save(alice, aliceFood, "80.00", LocalDate.of(2026, 3, 1), "Groceries");
        save(alice, aliceFood, "30.00", LocalDate.of(2026, 3, 31), "Dinner");
        save(alice, aliceFood, "40.00", LocalDate.of(2026, 4, 1), "April snacks");
        save(alice, aliceSalary, "2000.00", LocalDate.of(2026, 3, 1), "Paycheck");
        save(bob, bobFood, "500.00", LocalDate.of(2026, 3, 2), "Bob lunch");
    }

    @Test
    void filterIsInclusiveAndDoesNotLeakAnotherUsersRows() {
        List<Transaction> marchFood = transactionRepository.filter(
                alice, "Food", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThat(marchFood).extracting(Transaction::getDescription)
                .containsExactlyInAnyOrder("Groceries", "Dinner");
        assertThat(marchFood).allSatisfy(transaction -> assertThat(transaction.getUser().getId()).isEqualTo(alice.getId()));

        assertThat(transactionRepository.filter(alice, "food", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .isEmpty();
        assertThat(transactionRepository.filter(bob, "Food", null, null))
                .extracting(Transaction::getDescription)
                .containsExactly("Bob lunch");
    }

    @Test
    void filterWithoutCategoryOrDatesReturnsOnlyThatUsersTransactions() {
        assertThat(transactionRepository.filter(alice, null, null, null)).hasSize(4);
        assertThat(transactionRepository.findByUser(bob)).hasSize(1);
    }

    @Test
    void monthlyTotalsAndCategorySpendIgnoreOtherMonths() {
        assertThat(transactionRepository.getTotalIncomeForMonth(3, 2026)).isEqualTo(2000.0);
        // The month total is not filtered by user, so Bob's 500 is included with Alice's 110.
        assertThat(transactionRepository.getTotalExpenseForMonth(3, 2026)).isEqualTo(610.0);
        assertThat(transactionRepository.getTotalIncomeForMonth(1, 2020)).isNull();

        assertThat(transactionRepository.getMonthlySpentForCategory(aliceFood.getId(), 3, 2026)).isEqualTo(110.0);
        assertThat(transactionRepository.getYearlySpentForCategory(aliceFood.getId(), 2026)).isEqualTo(150.0);
        assertThat(transactionRepository.getMonthlySpentForCategory(bobFood.getId(), 4, 2026)).isNull();

        Map<String, Double> march = new HashMap<>();
        for (Object[] row : transactionRepository.getCategoryWiseTotals(3, 2026)) {
            march.put((String) row[0], ((Number) row[1]).doubleValue());
        }
        assertThat(march.get("Salary")).isEqualTo(2000.0);
        // Both users named their expense category "Food", and this query groups by name only.
        assertThat(march.get("Food")).isEqualTo(610.0);
    }

    private void save(User user, Category category, String amount, LocalDate date, String description) {
        transactionRepository.saveAndFlush(TestEntities.transaction(user, category, amount, date, description));
    }
}
