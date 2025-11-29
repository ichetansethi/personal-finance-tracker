package com.chetan.personalfinancetracker.mapper;

import com.chetan.personalfinancetracker.dto.TransactionDTO;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Transaction;
import com.chetan.personalfinancetracker.model.User;

public class TransactionMapper {

    public static TransactionDTO toDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setType(transaction.getType());
        dto.setDate(transaction.getDate());
        dto.setDescription(transaction.getDescription());

        if (transaction.getCategory() != null) {
            dto.setCategoryId(transaction.getCategory().getId());
            dto.setCategoryName(transaction.getCategory().getName());
        }

        if (transaction.getUser() != null) {
            dto.setUserId(transaction.getUser().getId());
        }

        return dto;
    }

    public static Transaction toEntity(TransactionDTO dto, Category category, User user) {
        Transaction transaction = new Transaction();
        transaction.setId(dto.getId()); // Optional: only set if updating
        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        transaction.setDate(dto.getDate());
        transaction.setDescription(dto.getDescription());
        transaction.setCategory(category);
        transaction.setUser(user);
        return transaction;
    }
}