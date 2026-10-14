package com.chetan.personalfinancetracker.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chetan.personalfinancetracker.TransactionType;
import com.chetan.personalfinancetracker.exception.ResourceNotFoundException;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.repository.CategoryRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void getAllCategoriesReturnsTheRepositoryContents() {
        Category rent = new Category("Rent", TransactionType.EXPENSE, null);
        when(categoryRepository.findAll()).thenReturn(List.of(rent));

        assertThat(categoryService.getAllCategories()).containsExactly(rent);
    }

    @Test
    void createCategorySavesTheGivenEntity() {
        Category rent = new Category("Rent", TransactionType.EXPENSE, null);
        when(categoryRepository.save(rent)).thenReturn(rent);

        assertThat(categoryService.createCategory(rent)).isSameAs(rent);
        verify(categoryRepository).save(rent);
    }

    @Test
    void findByIdReturnsTheCategory() {
        Category rent = new Category("Rent", TransactionType.EXPENSE, null);
        rent.setId(5L);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(rent));

        assertThat(categoryService.findById(5L)).isSameAs(rent);
    }

    @Test
    void findByIdRejectsAnUnknownId() {
        when(categoryRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with ID: 5");
    }
}
