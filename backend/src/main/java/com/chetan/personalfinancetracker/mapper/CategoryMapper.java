package com.chetan.personalfinancetracker.mapper;

import com.chetan.personalfinancetracker.TransactionType;
import com.chetan.personalfinancetracker.dto.CategoryDTO;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.User;

public class CategoryMapper {

    public static CategoryDTO toDTO(Category category) {
        CategoryDTO dto = new CategoryDTO(category.getId(), category.getName(), category.getType());
        dto.setMonthlyBudget(category.getMonthlyBudget());
        dto.setYearlyBudget(category.getYearlyBudget());
        return dto;
    }

    public static Category toEntity(CategoryDTO dto, User user) {
        Category category = new Category();
        category.setId(dto.getId());
        category.setName(dto.getName());
        if (dto.getType() != null) {
            category.setType(TransactionType.valueOf(dto.getType()));
        }
        category.setUser(user);
        category.setMonthlyBudget(dto.getMonthlyBudget());
        category.setYearlyBudget(dto.getYearlyBudget());
        return category;
    }

}
