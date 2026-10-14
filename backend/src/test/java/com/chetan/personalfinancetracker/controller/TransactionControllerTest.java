package com.chetan.personalfinancetracker.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import com.chetan.personalfinancetracker.dto.MonthlySummaryDTO;
import com.chetan.personalfinancetracker.dto.TransactionDTO;
import com.chetan.personalfinancetracker.exception.GlobalExceptionHandler;
import com.chetan.personalfinancetracker.exception.ResourceNotFoundException;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.Transaction;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.CategoryRepository;
import com.chetan.personalfinancetracker.repository.TransactionRepository;
import com.chetan.personalfinancetracker.repository.UserRepository;
import com.chetan.personalfinancetracker.security.JwtFilter;
import com.chetan.personalfinancetracker.security.JwtUtil;
import com.chetan.personalfinancetracker.service.TransactionService;
import com.chetan.personalfinancetracker.support.WebTestAuth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, JwtFilter.class, JwtUtil.class, CorsConfig.class,
        GlobalExceptionHandler.class, PasswordEncoderConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=0123456789abcdef0123456789abcdef")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void stubCaller() {
        WebTestAuth.stubUser(userDetailsService, "alice");
    }

    @Test
    void anonymousCallersCannotListTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createIgnoresAUserIdInTheBodyAndStoresTheCaller() throws Exception {
        User alice = user(7L, "alice");
        Category food = category(3L, "Food");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(food));
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(99L);
            return transaction;
        });

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":25.50,"type":"EXPENSE","date":"2026-03-01","description":"Lunch","categoryId":3,"userId":999}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.user").doesNotExist());

        org.mockito.ArgumentCaptor<Transaction> saved = org.mockito.ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(alice);
        assertThat(saved.getValue().getUser().getId()).isNotEqualTo(999L);
        assertThat(saved.getValue().getCategory()).isSameAs(food);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("25.50");
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidTransactions")
    void createRejectsInvalidInput(String body, String message) throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void createReportsAnUnknownCategory() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user(7L, "alice")));
        when(categoryRepository.findById(3L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10,"type":"EXPENSE","date":"2026-03-01","description":"Lunch","categoryId":3}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    void listLoadsOnlyTheAuthenticatedUsersTransactions() throws Exception {
        User alice = user(7L, "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        Transaction lunch = new Transaction();
        lunch.setId(1L);
        lunch.setDescription("Lunch");
        lunch.setAmount(new BigDecimal("10.00"));
        lunch.setDate(LocalDate.of(2026, 3, 1));
        lunch.setType(TransactionType.EXPENSE);
        when(transactionRepository.findByUser(alice)).thenReturn(List.of(lunch));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Lunch"))
                .andExpect(jsonPath("$[0].user").doesNotExist());

        verify(transactionRepository).findByUser(alice);
    }

    @Test
    void filterPassesTheCallerAndTheQueryWindowToTheService() throws Exception {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(4L);
        dto.setDescription("Lunch");
        dto.setAmount(new BigDecimal("12.00"));
        dto.setDate(LocalDate.of(2026, 3, 2));
        dto.setType(TransactionType.EXPENSE);
        dto.setCategoryName("Food");
        when(transactionService.filterTransactions("alice", "Food", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/transactions/filter")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .param("category", "Food")
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Lunch"))
                .andExpect(jsonPath("$[0].categoryName").value("Food"));
    }

    @Test
    void summaryReturnsIncomeExpenseAndNetBalance() throws Exception {
        when(transactionService.getMonthlySummary(3, 2026))
                .thenReturn(new MonthlySummaryDTO(1000, 400, Map.of("Food", 400.0), List.of()));

        mockMvc.perform(get("/api/transactions/summary")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .param("month", "3")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1000.0))
                .andExpect(jsonPath("$.totalExpense").value(400.0))
                .andExpect(jsonPath("$.netBalance").value(600.0))
                .andExpect(jsonPath("$.categorySummary.Food").value(400.0));
    }

    @Test
    void listReportsAnUnknownAuthenticatedAccount() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    private static Stream<Arguments> invalidTransactions() {
        return Stream.of(
                Arguments.of("{\"type\":\"EXPENSE\",\"date\":\"2026-03-01\",\"description\":\"Lunch\",\"categoryId\":3}",
                        "amount: Amount is required"),
                Arguments.of("{\"amount\":0,\"type\":\"EXPENSE\",\"date\":\"2026-03-01\",\"description\":\"Lunch\",\"categoryId\":3}",
                        "amount: Amount must be positive"),
                Arguments.of("{\"amount\":-5,\"type\":\"EXPENSE\",\"date\":\"2026-03-01\",\"description\":\"Lunch\",\"categoryId\":3}",
                        "amount: Amount must be positive"),
                Arguments.of("{\"amount\":10,\"date\":\"2026-03-01\",\"description\":\"Lunch\",\"categoryId\":3}",
                        "type: Transaction type is required"),
                Arguments.of("{\"amount\":10,\"type\":\"EXPENSE\",\"description\":\"Lunch\",\"categoryId\":3}",
                        "date: Date is required"),
                Arguments.of("{\"amount\":10,\"type\":\"EXPENSE\",\"date\":\"2026-03-01\",\"description\":\"  \",\"categoryId\":3}",
                        "description: Description is required"),
                Arguments.of("{\"amount\":10,\"type\":\"EXPENSE\",\"date\":\"2026-03-01\",\"description\":\"Lunch\"}",
                        "categoryId: Category is required"));
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("hash");
        return user;
    }

    private static Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(TransactionType.EXPENSE);
        return category;
    }
}
