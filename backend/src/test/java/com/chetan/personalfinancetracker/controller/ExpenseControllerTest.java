package com.chetan.personalfinancetracker.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.chetan.personalfinancetracker.TransactionType;
import com.chetan.personalfinancetracker.config.CorsConfig;
import com.chetan.personalfinancetracker.config.PasswordEncoderConfig;
import com.chetan.personalfinancetracker.config.SecurityConfig;
import com.chetan.personalfinancetracker.exception.GlobalExceptionHandler;
import com.chetan.personalfinancetracker.exception.ResourceNotFoundException;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Expense;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.ExpenseRepository;
import com.chetan.personalfinancetracker.repository.UserRepository;
import com.chetan.personalfinancetracker.security.JwtFilter;
import com.chetan.personalfinancetracker.security.JwtUtil;
import com.chetan.personalfinancetracker.service.CategoryService;
import com.chetan.personalfinancetracker.service.ExpenseService;
import com.chetan.personalfinancetracker.support.WebTestAuth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseController.class)
@Import({SecurityConfig.class, JwtFilter.class, JwtUtil.class, CorsConfig.class,
        GlobalExceptionHandler.class, PasswordEncoderConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=0123456789abcdef0123456789abcdef")
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private ExpenseRepository expenseRepository;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private ExpenseService expenseService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void stubCaller() {
        WebTestAuth.stubUser(userDetailsService, "alice");
    }

    @Test
    void anonymousCallersCannotReadExpenses() throws Exception {
        mockMvc.perform(get("/api/expenses/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createStoresTheExpenseAgainstTheAuthenticatedUser() throws Exception {
        User alice = user(7L, "alice");
        Category food = category(3L, "Food");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(categoryService.findById(3L)).thenReturn(food);
        when(expenseService.createExpense(any())).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            expense.setId(11L);
            return expense;
        });

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Lunch","amount":12.50,"date":"2026-03-02","categoryId":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.categoryName").value("Food"));

        org.mockito.ArgumentCaptor<Expense> saved = org.mockito.ArgumentCaptor.forClass(Expense.class);
        verify(expenseService).createExpense(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(alice);
        assertThat(saved.getValue().getCategory()).isSameAs(food);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("12.50");
    }

    @Test
    void createRejectsANonPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Lunch","amount":0,"date":"2026-03-02","categoryId":3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("amount: Amount must be positive"));
    }

    @Test
    void createReportsAnUnknownCategory() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user(7L, "alice")));
        when(categoryService.findById(3L)).thenThrow(new ResourceNotFoundException("Category not found with ID: 3"));

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Lunch","amount":12.50,"date":"2026-03-02","categoryId":3}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found with ID: 3"));
    }

    @Test
    void listUsesTheAuthenticatedUsersId() throws Exception {
        User alice = user(7L, "alice");
        Category food = category(3L, "Food");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        Expense lunch = new Expense();
        lunch.setId(11L);
        lunch.setDescription("Lunch");
        lunch.setAmount(new BigDecimal("12.50"));
        lunch.setDate(LocalDate.of(2026, 3, 2));
        lunch.setCategory(food);
        lunch.setUser(alice);
        when(expenseRepository.findByUserId(7L)).thenReturn(List.of(lunch));

        mockMvc.perform(get("/api/expenses/my")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Lunch"))
                .andExpect(jsonPath("$[0].category.name").value("Food"));

        verify(expenseRepository).findByUserId(7L);
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        return user;
    }

    private static Category category(Long id, String name) {
        Category category = new Category(name, TransactionType.EXPENSE, null);
        category.setId(id);
        return category;
    }
}
