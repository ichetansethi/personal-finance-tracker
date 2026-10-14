package com.chetan.personalfinancetracker.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.chetan.personalfinancetracker.config.CorsConfig;
import com.chetan.personalfinancetracker.config.PasswordEncoderConfig;
import com.chetan.personalfinancetracker.config.SecurityConfig;
import com.chetan.personalfinancetracker.dto.MonthlySummaryDTO;
import com.chetan.personalfinancetracker.dto.MonthlySummaryDTO.BudgetAlert;
import com.chetan.personalfinancetracker.exception.GlobalExceptionHandler;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.UserRepository;
import com.chetan.personalfinancetracker.security.JwtFilter;
import com.chetan.personalfinancetracker.security.JwtUtil;
import com.chetan.personalfinancetracker.service.TransactionService;
import com.chetan.personalfinancetracker.support.WebTestAuth;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SummaryController.class)
@Import({SecurityConfig.class, JwtFilter.class, JwtUtil.class, CorsConfig.class,
        GlobalExceptionHandler.class, PasswordEncoderConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=0123456789abcdef0123456789abcdef")
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void stubCaller() {
        WebTestAuth.stubUser(userDetailsService, "alice");
    }

    @Test
    void anonymousCallersCannotReadBudgetAlerts() throws Exception {
        mockMvc.perform(get("/api/summary/budget-alerts").param("month", "3").param("year", "2026"))
                .andExpect(status().isForbidden());
    }

    @Test
    void budgetAlertsAreLoadedForTheAuthenticatedUser() throws Exception {
        User alice = new User();
        alice.setId(9L);
        alice.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(transactionService.getBudgetAlerts(9L, 3, 2026))
                .thenReturn(List.of(new BudgetAlert("Food", 120.0, 100.0, true, "Monthly")));

        mockMvc.perform(get("/api/summary/budget-alerts")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .param("month", "3")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Food"))
                .andExpect(jsonPath("$[0].spent").value(120.0))
                .andExpect(jsonPath("$[0].budget").value(100.0))
                .andExpect(jsonPath("$[0].exceeded").value(true))
                .andExpect(jsonPath("$[0].period").value("Monthly"));

        verify(transactionService).getBudgetAlerts(9L, 3, 2026);
    }

    @Test
    void monthlySummaryReturnsTheServicePayload() throws Exception {
        when(transactionService.getMonthlySummary(3, 2026))
                .thenReturn(new MonthlySummaryDTO(500, 125, Map.of("Rent", 125.0), List.of()));

        mockMvc.perform(get("/api/summary/monthly")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .param("month", "3")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(500.0))
                .andExpect(jsonPath("$.netBalance").value(375.0));
    }

    @Test
    void yearlySummaryDelegatesWithMonthZero() throws Exception {
        // The yearly endpoint currently reuses the monthly query with month 0.
        when(transactionService.getMonthlySummary(0, 2026))
                .thenReturn(new MonthlySummaryDTO(0, 0, Map.of(), List.of()));

        mockMvc.perform(get("/api/summary/yearly")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0.0));

        verify(transactionService).getMonthlySummary(0, 2026);
    }
}
