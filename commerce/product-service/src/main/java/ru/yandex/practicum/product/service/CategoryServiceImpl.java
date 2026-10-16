package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.repository.CategoryMapper;
import ru.yandex.practicum.product.repository.CategoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl extends BaseService implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryDto> getAllCategories() {
        log.trace(" getAllCategories");
        List<Category> categories = categoryRepository.findAll();
        log.debug("OK {}", categories);
        return categories.stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
    }

    @Override
    public CategoryDto createCategory(CreateCategoryRequest request) {
        log.trace("createCategory {}", request);
        Category category = CategoryMapper.toCategory(request);
        Category savedCategory = categoryRepository.save(category);
        log.debug("OK created {}", category);
        return CategoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    public CategoryDto getCategory(long categoryId) {
        log.trace("getCategory {}", categoryId);
        Category category = findEntityIn(categoryRepository, Category.class.getName(), categoryId);
        log.debug("Ok {}", categoryId);
        return CategoryMapper.toCategoryDto(category);
    }
}
