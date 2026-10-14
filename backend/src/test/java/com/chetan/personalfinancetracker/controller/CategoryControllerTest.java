package com.chetan.personalfinancetracker.controller;

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
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.UserRepository;
import com.chetan.personalfinancetracker.security.JwtFilter;
import com.chetan.personalfinancetracker.security.JwtUtil;
import com.chetan.personalfinancetracker.service.CategoryService;
import com.chetan.personalfinancetracker.support.WebTestAuth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtFilter.class, JwtUtil.class, CorsConfig.class,
        GlobalExceptionHandler.class, PasswordEncoderConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=0123456789abcdef0123456789abcdef")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void stubCaller() {
        WebTestAuth.stubUser(userDetailsService, "alice");
    }

    @Test
    void anonymousCallersCannotCreateCategories() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rent","type":"EXPENSE","monthlyBudget":100,"yearlyBudget":1200}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAttachesTheAuthenticatedUserAndIgnoresABodyUserId() throws Exception {
        User alice = new User();
        alice.setId(4L);
        alice.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(categoryService.createCategory(any())).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(5L);
            return category;
        });

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rent","type":"EXPENSE","monthlyBudget":0,"yearlyBudget":1200,"userId":999}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Rent"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.monthlyBudget").value(0.0))
                .andExpect(jsonPath("$.yearlyBudget").value(1200.0));

        org.mockito.ArgumentCaptor<Category> saved = org.mockito.ArgumentCaptor.forClass(Category.class);
        verify(categoryService).createCategory(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(alice);
        assertThat(saved.getValue().getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void createRejectsABlankName() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","type":"EXPENSE","monthlyBudget":100}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name: Category name is required"));
    }

    @Test
    void createRejectsANegativeBudget() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rent","type":"EXPENSE","monthlyBudget":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("monthlyBudget: Monthly budget must be zero or positive"));
    }

    @Test
    void createRejectsAnUnknownType() throws Exception {
        User alice = new User();
        alice.setId(4L);
        alice.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rent","type":"RENT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No enum constant com.chetan.personalfinancetracker.TransactionType.RENT"));
    }

    @Test
    void listReturnsOnlyTheAuthenticatedUsersCategories() throws Exception {
        User alice = new User();
        alice.setId(4L);
        alice.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        Category rent = new Category("Rent", TransactionType.EXPENSE, alice);
        rent.setId(5L);
        rent.setMonthlyBudget(100.0);
        when(categoryService.getCategoriesForUser(4L)).thenReturn(List.of(rent));

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Rent"))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$[0].monthlyBudget").value(100.0))
                .andExpect(jsonPath("$[0].password").doesNotExist());

        verify(categoryService).getCategoriesForUser(4L);
    }
}
