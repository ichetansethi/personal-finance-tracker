package com.chetan.personalfinancetracker.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.chetan.personalfinancetracker.TransactionType;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.support.TestEntities;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void categoriesAreListedPerOwner() {
        User alice = userRepository.saveAndFlush(TestEntities.user("alice"));
        User bob = userRepository.saveAndFlush(TestEntities.user("bob"));
        categoryRepository.saveAndFlush(TestEntities.category("Rent", TransactionType.EXPENSE, alice, 100.0, 1200.0));
        categoryRepository.saveAndFlush(TestEntities.category("Salary", TransactionType.INCOME, alice, null, null));
        categoryRepository.saveAndFlush(TestEntities.category("Food", TransactionType.EXPENSE, bob, 40.0, 400.0));

        assertThat(categoryRepository.findByUserId(alice.getId())).extracting(Category::getName)
                .containsExactlyInAnyOrder("Rent", "Salary");
        assertThat(categoryRepository.findByUserId(bob.getId())).extracting(Category::getName)
                .containsExactly("Food");
        assertThat(categoryRepository.findByName("Rent").getMonthlyBudget()).isEqualTo(100.0);
        assertThat(categoryRepository.findByName("missing")).isNull();
    }
}
