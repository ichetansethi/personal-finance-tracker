package com.chetan.personalfinancetracker.integration;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FinanceTrackerFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void registerTwoAccounts() throws Exception {
        register("alice", "password1", "alice@example.com");
        register("bob", "password2", "bob@example.com");
        aliceToken = login("alice", "password1");
        bobToken = login("bob", "password2");
    }

    @Test
    void duplicateUsernameAndBadPasswordAreRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson("alice", "password1", "other@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson("alice", "wrong-password", "alice@example.com")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void anonymousAndInvalidTransactionInputAreRejected() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        long categoryId = createCategory(aliceToken, "Rent", "EXPENSE", 100, 10000);

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":-1,"type":"EXPENSE","date":"2026-03-15","description":"Rent","categoryId":%d}
                                """.formatted(categoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("amount: Amount must be positive"));
    }

    @Test
    void usersCannotReadEachOthersTransactions() throws Exception {
        long aliceRent = createCategory(aliceToken, "Rent", "EXPENSE", 100, 10000);
        long bobFood = createCategory(bobToken, "Food", "EXPENSE", 40, 400);
        createTransaction(aliceToken, aliceRent, "150.00", "March rent");
        createTransaction(bobToken, bobFood, "10.00", "Lunch");

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("March rent"));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Lunch"));

        mockMvc.perform(get("/api/transactions/filter")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("category", "Food")
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/transactions/filter")
                        .header("Authorization", "Bearer " + bobToken)
                        .param("category", "Food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Lunch"))
                .andExpect(jsonPath("$[0].categoryName").value("Food"));
    }

    @Test
    void budgetAlertsAreScopedToTheCallerAndFlagSpendOverTheLimit() throws Exception {
        long aliceRent = createCategory(aliceToken, "Rent", "EXPENSE", 100, 10000);
        createCategory(bobToken, "Food", "EXPENSE", 40, 400);
        createTransaction(aliceToken, aliceRent, "150.00", "March rent");

        List<Map<String, Object>> aliceAlerts = alerts(aliceToken);
        Map<String, Object> aliceMonthly = alert(aliceAlerts, "Rent", "Monthly");
        Map<String, Object> aliceYearly = alert(aliceAlerts, "Rent", "Yearly");
        assertThat(((Number) aliceMonthly.get("spent")).doubleValue()).isEqualTo(150.0);
        assertThat(aliceMonthly.get("exceeded")).isEqualTo(true);
        assertThat(((Number) aliceYearly.get("spent")).doubleValue()).isEqualTo(150.0);
        assertThat(aliceYearly.get("exceeded")).isEqualTo(false);
        assertThat(aliceAlerts).extracting(alert -> alert.get("category")).containsOnly("Rent");

        List<Map<String, Object>> bobAlerts = alerts(bobToken);
        Map<String, Object> bobMonthly = alert(bobAlerts, "Food", "Monthly");
        assertThat(((Number) bobMonthly.get("spent")).doubleValue()).isEqualTo(0.0);
        assertThat(bobMonthly.get("exceeded")).isEqualTo(false);
        assertThat(bobAlerts).extracting(alert -> alert.get("category")).containsOnly("Food");
    }

    @Test
    void categoriesSummariesAndWritesStayWithTheOwner() throws Exception {
        long aliceRent = createCategory(aliceToken, "Rent", "EXPENSE", 100, 10000);
        long bobFood = createCategory(bobToken, "Food", "EXPENSE", 40, 400);
        createTransaction(aliceToken, aliceRent, "150.00", "March rent");
        createTransaction(bobToken, bobFood, "10.00", "Lunch");

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Rent"));

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Food"));

        mockMvc.perform(get("/api/summary/monthly")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("month", "3")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(150.0))
                .andExpect(jsonPath("$.categorySummary.Rent").value(150.0))
                .andExpect(jsonPath("$.categorySummary.Food").doesNotExist());

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":20,"type":"EXPENSE","date":"2026-04-02","description":"April rent","categoryId":%d}
                                """.formatted(aliceRent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Rent"))
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/summary/monthly")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("month", "3")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(150.0));

        mockMvc.perform(get("/api/summary/yearly")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(170.0))
                .andExpect(jsonPath("$.categorySummary.Rent").value(170.0));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":5,"type":"EXPENSE","date":"2026-03-15","description":"Not mine","categoryId":%d}
                                """.formatted(aliceRent)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Not mine","amount":5,"date":"2026-03-15","categoryId":%d}
                                """.formatted(aliceRent)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));

        mockMvc.perform(post("/api/incomes")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"source":"Side gig","amount":5,"date":"2026-03-15","categoryId":%d}
                                """.formatted(aliceRent)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"March rent","amount":15,"date":"2026-03-16","categoryId":%d}
                                """.formatted(aliceRent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Rent"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/expenses/my")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("Rent"))
                .andExpect(jsonPath("$[0].user").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());

        long alicePay = createCategory(aliceToken, "Payroll", "INCOME", 0, 0);
        mockMvc.perform(post("/api/incomes")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"source":"Salary","amount":2000,"date":"2026-03-01","categoryId":%d}
                                """.formatted(alicePay)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Payroll"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/incomes/user")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("Payroll"))
                .andExpect(jsonPath("$[0].user").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void clientMistakesAndDisabledOtpReturn4xx() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"cara","password":"password1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email: Email is required"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson("cara", "password1", "alice@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","otp":"123456"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("OTP verification is disabled"))
                .andExpect(jsonPath("$.token").doesNotExist());

        mockMvc.perform(get("/api/summary/monthly")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("year", "2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("month is required"));

        mockMvc.perform(get("/api/summary/monthly")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("month", "abc")
                        .param("year", "2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("month is invalid"));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    private void register(String username, String password, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(username, password, email)))
                .andExpect(status().isOk());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(username, password, username + "@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpRequired").value(false))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private long createCategory(String token, String name, String type, double monthly, double yearly) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","type":"%s","monthlyBudget":%s,"yearlyBudget":%s,"userId":999}
                                """.formatted(name, type, monthly, yearly)))
                .andExpect(status().isOk())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private void createTransaction(String token, long categoryId, String amount, String description) throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":%s,"type":"EXPENSE","date":"2026-03-15","description":"%s","categoryId":%d,"userId":999}
                                """.formatted(amount, description, categoryId)))
                .andExpect(status().isOk());
    }

    private List<Map<String, Object>> alerts(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/summary/budget-alerts")
                        .header("Authorization", "Bearer " + token)
                        .param("month", "3")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$");
    }

    private static Map<String, Object> alert(List<Map<String, Object>> alerts, String category, String period) {
        return alerts.stream()
                .filter(alert -> category.equals(alert.get("category")) && period.equals(alert.get("period")))
                .findFirst()
                .orElseThrow();
    }

    private static String accountJson(String username, String password, String email) {
        return """
                {"username":"%s","password":"%s","email":"%s"}
                """.formatted(username, password, email);
    }
}
