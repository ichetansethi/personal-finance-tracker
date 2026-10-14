package com.chetan.personalfinancetracker.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.chetan.personalfinancetracker.TransactionType;
import com.chetan.personalfinancetracker.dto.CategoryDTO;
import com.chetan.personalfinancetracker.dto.TransactionDTO;
import com.chetan.personalfinancetracker.dto.UserDTO;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Transaction;
import com.chetan.personalfinancetracker.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MapperTest {

    @Test
    void transactionMapperRoundTripsOwnedFields() {
        User user = new User();
        user.setId(4L);
        Category category = new Category("Food", TransactionType.EXPENSE, user);
        category.setId(9L);

        TransactionDTO dto = new TransactionDTO();
        dto.setAmount(new BigDecimal("18.25"));
        dto.setType(TransactionType.EXPENSE);
        dto.setDate(LocalDate.of(2026, 3, 2));
        dto.setDescription("Lunch");

        Transaction entity = TransactionMapper.toEntity(dto, category, user);
        entity.setId(3L);
        TransactionDTO mapped = TransactionMapper.toDTO(entity);

        assertThat(mapped.getId()).isEqualTo(3L);
        assertThat(mapped.getAmount()).isEqualByComparingTo("18.25");
        assertThat(mapped.getCategoryId()).isEqualTo(9L);
        assertThat(mapped.getCategoryName()).isEqualTo("Food");
        assertThat(mapped.getUserId()).isEqualTo(4L);
    }

    @Test
    void transactionMapperOmitsMissingAssociations() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAmount(BigDecimal.ONE);
        transaction.setDescription("Cash");
        transaction.setDate(LocalDate.of(2026, 1, 1));
        transaction.setType(TransactionType.INCOME);

        TransactionDTO dto = TransactionMapper.toDTO(transaction);

        assertThat(dto.getCategoryId()).isNull();
        assertThat(dto.getCategoryName()).isNull();
        assertThat(dto.getUserId()).isNull();
    }

    @Test
    void categoryMapperCopiesBudgetsAndParsesType() {
        User user = new User();
        user.setId(2L);
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Rent");
        dto.setType("EXPENSE");
        dto.setMonthlyBudget(100.0);
        dto.setYearlyBudget(1200.0);

        Category entity = CategoryMapper.toEntity(dto, user);
        entity.setId(6L);
        CategoryDTO mapped = CategoryMapper.toDTO(entity);

        assertThat(entity.getUser()).isSameAs(user);
        assertThat(entity.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(mapped.getId()).isEqualTo(6L);
        assertThat(mapped.getName()).isEqualTo("Rent");
        assertThat(mapped.getType()).isEqualTo("EXPENSE");
        assertThat(mapped.getMonthlyBudget()).isEqualTo(100.0);
        assertThat(mapped.getYearlyBudget()).isEqualTo(1200.0);
    }

    @Test
    void categoryMapperRejectsAnUnknownType() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Rent");
        dto.setType("RENT");

        assertThatThrownBy(() -> CategoryMapper.toEntity(dto, new User()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userMapperDoesNotExposeThePassword() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPassword("$2a$hash");

        UserDTO dto = UserMapper.toDTO(user);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
    }
}
