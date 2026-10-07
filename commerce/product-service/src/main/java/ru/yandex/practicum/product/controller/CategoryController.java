package ru.yandex.practicum.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.service.CategoryService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ControllerConstants.URL_API + ControllerConstants.URL_CATEGORIES)
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDto> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping(ControllerConstants.ID_PARAM)
    public CategoryDto getCategory(@PathVariable(name = ControllerConstants.ID) long categoryId) {
        return categoryService.getCategory(categoryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(request);
    }
}
