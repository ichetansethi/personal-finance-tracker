package com.chetan.personalfinancetracker.mapper;

import com.chetan.personalfinancetracker.dto.UserDTO;
import com.chetan.personalfinancetracker.model.User;

public class UserMapper {

    public static UserDTO toDTO(User user) {
        return new UserDTO(
            user.getId(),
            user.getUsername(),
            user.getEmail()
        );
    }
}
