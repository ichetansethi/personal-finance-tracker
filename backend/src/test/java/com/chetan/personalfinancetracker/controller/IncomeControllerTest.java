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
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Income;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.IncomeRepository;
import com.chetan.personalfinancetracker.repository.UserRepository;
import com.chetan.personalfinancetracker.security.JwtFilter;
import com.chetan.personalfinancetracker.security.JwtUtil;
import com.chetan.personalfinancetracker.service.CategoryService;
import com.chetan.personalfinancetracker.service.IncomeService;
import com.chetan.personalfinancetracker.support.WebTestAuth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncomeController.class)
@Import({SecurityConfig.class, JwtFilter.class, JwtUtil.class, CorsConfig.class,
        GlobalExceptionHandler.class, PasswordEncoderConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=0123456789abcdef0123456789abcdef")
class IncomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private IncomeRepository incomeRepository;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private IncomeService incomeService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void stubCaller() {
        WebTestAuth.stubUser(userDetailsService, "alice");
    }

    @Test
    void anonymousCallersCannotReadIncome() throws Exception {
        mockMvc.perform(get("/api/incomes/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createStoresIncomeForTheAuthenticatedUser() throws Exception {
        User alice = user(7L, "alice");
        Category payroll = new Category("Payroll", TransactionType.INCOME, alice);
        payroll.setId(2L);
        when(categoryService.findOwnedCategory(2L, 7L)).thenReturn(payroll);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(incomeService.createIncome(any())).thenAnswer(invocation -> {
            Income income = invocation.getArgument(0);
            income.setId(8L);
            return income;
        });

        mockMvc.perform(post("/api/incomes")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"source":"Salary","amount":2000,"date":"2026-03-01","categoryId":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.source").value("Salary"))
                .andExpect(jsonPath("$.categoryName").value("Payroll"))
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        org.mockito.ArgumentCaptor<Income> saved = org.mockito.ArgumentCaptor.forClass(Income.class);
        verify(incomeService).createIncome(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(alice);
        assertThat(saved.getValue().getCategory()).isEqualTo("Payroll");
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("2000");
    }

    @Test
    void createRejectsABlankSource() throws Exception {
        mockMvc.perform(post("/api/incomes")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"source":" ","amount":2000,"date":"2026-03-01","categoryId":2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("source: Source is required"));
    }

    @Test
    void listUsesTheAuthenticatedUsersId() throws Exception {
        User alice = user(7L, "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        Income salary = new Income();
        salary.setId(8L);
        salary.setSource("Salary");
        salary.setAmount(new BigDecimal("2000"));
        salary.setDate(LocalDate.of(2026, 3, 1));
        salary.setCategory("Payroll");
        alice.setPassword("$2a$hash");
        salary.setUser(alice);
        when(incomeRepository.findByUserId(7L)).thenReturn(List.of(salary));

        mockMvc.perform(get("/api/incomes/user")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("Salary"))
                .andExpect(jsonPath("$[0].categoryName").value("Payroll"))
                .andExpect(jsonPath("$[0].category").doesNotExist())
                .andExpect(jsonPath("$[0].user").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("$2a$hash"))));

        verify(incomeRepository).findByUserId(7L);
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        return user;
    }
}
