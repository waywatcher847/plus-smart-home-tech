package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.product.dto.*;
import ru.yandex.practicum.product.entity.*;
import ru.yandex.practicum.product.repository.*;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl extends BaseService implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public List<ProductDto> getAllActiveProducts() {
        log.trace("getAllActiveProducts");
        List<Product> products = productRepository.findByActiveTrue();
        log.debug("OK {}", products);
        return convertToListProductDto(products);
    }

    @Override
    public ProductDto getProductById(long productId) {
        log.trace("getProductById {}", productId);
        Product product = findEntityIn(productRepository, Product.class.getName(), productId);
        log.debug("Ok {}", product);
        CategoryDto categoryDto = CategoryMapper.toCategoryDto(product.getCategory());
        return ProductMapper.toProductDto(product, categoryDto);
    }

    @Override
    public List<ProductDto> getProductsByCategory(long categoryId) {
        log.trace("getProductsByCategory {}", categoryId);
        List<Product> products = productRepository.findByCategoryId(categoryId);
        log.debug("OK {}", products);
        return convertToListProductDto(products);
    }

    @Override
    public ProductDto createProduct(CreateProductRequest request) {
        log.trace("createProduct {}", request);
        Category category = findEntityIn(categoryRepository, Category.class.getName(), request.categoryId());
        Product product = ProductMapper.toProduct(request, category);
        Product savedProduct = productRepository.save(product);
        log.debug("OK created {}", savedProduct);
        CategoryDto categoryDto = CategoryMapper.toCategoryDto(savedProduct.getCategory());
        return ProductMapper.toProductDto(savedProduct, categoryDto);
    }

    @Override
    public ProductDto updateProduct(long productId, UpdateProductRequest request) {
        log.trace("updateProduct {}, {}", productId, request);
        Product product = findEntityIn(productRepository, Product.class.getName(), productId);

        Category category = null;

        if (request.categoryId() != null) {
            category = findEntityIn(categoryRepository, Category.class.getName(), request.categoryId());
        }

        updateProductFields(product, request, category);
        Product updatedProduct = productRepository.save(product);
        log.debug("OK {}", updatedProduct);
        CategoryDto categoryDto = CategoryMapper.toCategoryDto(updatedProduct.getCategory());
        return ProductMapper.toProductDto(updatedProduct, categoryDto);
    }

    @Override
    public List<ProductDto> searchProducts(String query) {
        log.trace("searchProducts {}", query);
        List<Product> products = productRepository.findByNameContainingIgnoreCase(query);
        log.debug("OK {}", products);
        return convertToListProductDto(products);
    }

    private List<ProductDto> convertToListProductDto(List<Product> products) {
        return products.stream()
                .map(product -> {
                    CategoryDto categoryDto = CategoryMapper.toCategoryDto(product.getCategory());
                    return ProductMapper.toProductDto(product, categoryDto);
                })
                .toList();
    }

    private void updateProductFields(Product product, UpdateProductRequest request, Category category) {
        if (request.name() != null) {
            product.setName(request.name());
        }

        if (request.description() != null) {
            product.setDescription(request.description());
        }

        if (request.price() != null) {
            product.setPrice(request.price());
        }

        if (category != null) {
            product.setCategory(category);
        }

        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
        }

        if (request.active() != null) {
            product.setActive(request.active());
        }
    }
}
