package ru.yandex.practicum.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.service.ProductService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ControllerConstants.URL_API + ControllerConstants.URL_PRODUCTS)
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public List<ProductDto> getAllActiveProducts() {
        return productService.getAllActiveProducts();
    }

    @GetMapping(ControllerConstants.ID_PARAM)
    public ProductDto getProductById(@PathVariable(name = ControllerConstants.ID) long productId) {
        return productService.getProductById(productId);
    }

    @GetMapping(ControllerConstants.URL_SEARCH)
    public List<ProductDto> searchProducts(@RequestParam String query) {
        return productService.searchProducts(query);
    }

    @GetMapping(ControllerConstants.URL_CATEGORY + ControllerConstants.ID_CATEGORY)
    public List<ProductDto> getProductsByCategory(@PathVariable long categoryId) {
        return productService.getProductsByCategory(categoryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(@Valid @RequestBody CreateProductRequest request) {
        return productService.createProduct(request);
    }

    @PatchMapping(ControllerConstants.ID_PARAM)
    public ProductDto updateProduct(@PathVariable(name = ControllerConstants.ID) long productId,
                                    @Valid @RequestBody UpdateProductRequest request) {
        return productService.updateProduct(productId, request);
    }

}
