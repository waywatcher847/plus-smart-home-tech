package ru.yandex.practicum.product.repository;

import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.entity.Category;

public class CategoryMapper {

    public static Category toCategory(CreateCategoryRequest request) {
        return Category.builder().name(request.name()).description(request.description()).build();
    }

    public static CategoryDto toCategoryDto(Category category) {
        return CategoryDto.builder().id(category.getId()).name(category.getName()).description(category.getDescription()).build();
    }
}
