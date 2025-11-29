package com.chetan.personalfinancetracker.mapper;

import com.chetan.personalfinancetracker.dto.IncomeDTO;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Income;

public class IncomeMapper {

    public static IncomeDTO toDTO(Income income) {
        IncomeDTO dto = new IncomeDTO();
        dto.setId(income.getId());
        dto.setSource(income.getSource());
        dto.setAmount(income.getAmount());
        dto.setDate(income.getDate());
        dto.setCategoryId(income.getId());
        dto.setCategoryName(income.getCategory());
        return dto;
    }

    public static Income toEntity(IncomeDTO dto, Category category) {
        Income income = new Income();
        income.setId(dto.getId());
        income.setSource(dto.getSource());
        income.setAmount(dto.getAmount());
        income.setDate(dto.getDate());
        income.setCategory(income.getCategory());
        return income;
    }
}