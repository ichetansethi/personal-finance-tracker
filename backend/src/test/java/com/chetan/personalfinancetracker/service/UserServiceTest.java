package com.chetan.personalfinancetracker.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserSavesTheAccount() {
        User user = new User();
        user.setUsername("alice");
        when(userRepository.save(user)).thenReturn(user);

        assertThat(userService.createUser(user)).isSameAs(user);
    }

    @Test
    void findByUsernameReturnsAMatch() {
        User user = new User();
        user.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThat(userService.findByUsername("alice")).contains(user);
    }

    @Test
    void findByUsernameIsEmptyWhenTheAccountDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThat(userService.findByUsername("missing")).isEmpty();
    }
}
