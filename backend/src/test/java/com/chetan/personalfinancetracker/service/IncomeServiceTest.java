package com.chetan.personalfinancetracker.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chetan.personalfinancetracker.model.Income;
import com.chetan.personalfinancetracker.repository.IncomeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncomeServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    @InjectMocks
    private IncomeService incomeService;

    @Test
    void createIncomeSavesTheEntry() {
        Income income = new Income();
        income.setSource("Salary");
        when(incomeRepository.save(income)).thenReturn(income);

        assertThat(incomeService.createIncome(income)).isSameAs(income);
    }

    @Test
    void getAllIncomesReturnsTheRepositoryContents() {
        when(incomeRepository.findAll()).thenReturn(List.of(new Income()));

        assertThat(incomeService.getAllIncomes()).hasSize(1);
    }
}
