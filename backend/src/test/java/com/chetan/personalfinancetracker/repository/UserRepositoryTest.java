package com.chetan.personalfinancetracker.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.support.TestEntities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findsAccountsByUsernameAndEmail() {
        User alice = userRepository.saveAndFlush(TestEntities.user("alice"));

        assertThat(userRepository.findByUsername("alice")).contains(alice);
        assertThat(userRepository.findByUsername("missing")).isEmpty();
        assertThat(userRepository.findByEmail("alice@example.com")).contains(alice);
        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("other@example.com")).isFalse();
    }

    @Test
    void usernameMustBeUnique() {
        userRepository.saveAndFlush(TestEntities.user("alice"));
        User duplicate = TestEntities.user("bob");
        duplicate.setUsername("alice");

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailMustBeUnique() {
        userRepository.saveAndFlush(TestEntities.user("alice"));
        User duplicate = TestEntities.user("bob");
        duplicate.setEmail("alice@example.com");

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
