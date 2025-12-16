package com.chetan.personalfinancetracker.controller;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chetan.personalfinancetracker.dto.CategoryDTO;
import com.chetan.personalfinancetracker.exception.ResourceNotFoundException;
import com.chetan.personalfinancetracker.mapper.CategoryMapper;
import com.chetan.personalfinancetracker.model.Category;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.UserRepository;
import com.chetan.personalfinancetracker.service.CategoryService;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        List<CategoryDTO> categories = categoryService.getAllCategories()
                .stream()
                .map(CategoryMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@RequestBody CategoryDTO dto, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = CategoryMapper.toEntity(dto, user);
        Category savedCategory = categoryService.createCategory(category);
        return ResponseEntity.ok(CategoryMapper.toDTO(savedCategory));
    }

}