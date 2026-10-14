package com.chetan.personalfinancetracker.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.chetan.personalfinancetracker.model.Income;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.support.TestEntities;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IncomeRepositoryTest {

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void incomesAreIsolatedByUserId() {
        User alice = userRepository.saveAndFlush(TestEntities.user("alice"));
        User bob = userRepository.saveAndFlush(TestEntities.user("bob"));
        incomeRepository.saveAndFlush(income(alice, "Salary", "2000.00"));
        incomeRepository.saveAndFlush(income(bob, "Freelance", "800.00"));

        assertThat(incomeRepository.findByUserId(alice.getId())).extracting(Income::getSource)
                .containsExactly("Salary");
        assertThat(incomeRepository.findByUserId(bob.getId())).extracting(Income::getSource)
                .containsExactly("Freelance");
    }

    private static Income income(User user, String source, String amount) {
        Income income = new Income();
        income.setUser(user);
        income.setSource(source);
        income.setAmount(new BigDecimal(amount));
        income.setDate(LocalDate.of(2026, 3, 1));
        income.setCategory(source);
        income.setDescription(source);
        return income;
    }
}
