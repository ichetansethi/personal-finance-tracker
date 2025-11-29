package com.chetan.personalfinancetracker.mapper;

import com.chetan.personalfinancetracker.dto.CategoryDTO;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.User;

public class CategoryMapper {

    public static CategoryDTO toDTO(Category category) {
        return new CategoryDTO(category.getId(), category.getName(), category.getType());
    }

    public static Category toEntity(CategoryDTO dto) {
        Category category = new Category();
        category.setId(dto.getId());
        category.setName(dto.getName());
        category.setType(dto.getType());
        dto.setMonthlyBudget(category.getMonthlyBudget());
        dto.setYearlyBudget(category.getYearlyBudget());
        return category;
    }

    public static Category toEntity(CategoryDTO dto, User user) {
        Category category = new Category();
        category.setId(dto.getId());
        category.setName(dto.getName());
        category.setType(dto.getType());
        category.setUser(user);
        dto.setMonthlyBudget(category.getMonthlyBudget());
        dto.setYearlyBudget(category.getYearlyBudget());
        return category;
    }
    
}
