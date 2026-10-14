package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.OrderStatus;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.fallback.InventoryAdapter;
import ru.yandex.practicum.order.fallback.ProductAdapter;
import ru.yandex.practicum.order.fallback.RemoteCallResult;
import ru.yandex.practicum.order.feign.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import ru.yandex.practicum.order.feign.ReleaseRequest;
import ru.yandex.practicum.order.feign.ReserveRequest;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderOrchestrationService {
    private final OrderService orderService;
    private final ProductAdapter productAdapter;
    private final InventoryAdapter inventoryAdapter;

    public OrderDto createOrder(CreateOrderRequest request) {
        log.trace("createOrder {}", request);
        AtomicBoolean isProductServiceDown = new AtomicBoolean(false);
        AtomicBoolean isInventoryServiceDown = new AtomicBoolean(false);

        Map<Long, ProductDto> productsMap = getUniqueProducts(request, isProductServiceDown);
        log.debug("productsMap {}", productsMap);

        if (isProductServiceDown.get()) {
            log.info("Order PENDING_CONFIRMATION status");
            OrderDto order = orderService.saveOrder(request, productsMap, OrderStatus.PENDING_CONFIRMATION);
            log.debug("OK created {}", order);
            return order;
        }

        List<Long> nonActiveProducts = findNonActiveProducts(productsMap);

        if (!nonActiveProducts.isEmpty()) {
            String ids = nonActiveProducts.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            log.warn("Products with ids: {} are inactive", ids);
            throw new OrderProcessingException(String.format("Products with ids: %s are inactive", ids));
        }

        reserveProducts(request, isInventoryServiceDown);

        if (isInventoryServiceDown.get()) {
            log.info("Order PENDING_CONFIRMATION status");

            Map<Long, ProductDto> updatedMap = productsMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> withDescription(
                                    entry.getValue(),
                                    entry.getValue().description() + " Reservation failed, manual check required"
                            )
                    ));

            OrderDto order = orderService.saveOrder(request, updatedMap, OrderStatus.PENDING_CONFIRMATION);
            log.debug("OK created {}", order);
            return order;
        }

        OrderDto order = orderService.saveOrder(request, productsMap, OrderStatus.CONFIRMED);
        log.debug("OK created {}", order);
        return order;
    }

    private Map<Long, ProductDto> getUniqueProducts(CreateOrderRequest request, AtomicBoolean pendingConfirmation) {
        log.trace("getUniqueProducts {}", request);
        Map<Long, ProductDto> result = request.items().stream()
                .map(OrderItemRequest::productId)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        productId -> getProduct(productId, pendingConfirmation)
                ));
        log.debug("OK {}", result);
        return result;
    }

    private List<Long> findNonActiveProducts(Map<Long, ProductDto> productsMap) {
        log.trace("findNonActiveProducts {}", productsMap);
        List<Long> result = productsMap.values().stream()
                .filter(product -> !product.active())
                .map(ProductDto::id)
                .toList();
        log.debug("OK {}", result);
        return result;
    }

    private void reserveProducts(CreateOrderRequest request, AtomicBoolean isInventoryServiceDown) {
        log.trace("reserveProducts {}", request);
        List<ReserveRequest> reservedOrders = new ArrayList<>();

        try {
            request.items().stream()
                    .collect(Collectors.groupingBy(
                            OrderItemRequest::productId,
                            Collectors.summingInt(OrderItemRequest::quantity)
                    ))
                    .entrySet().stream()
                    .map(entry -> new ReserveRequest(entry.getKey(), entry.getValue()))
                    .forEach(reserveRequest -> {
                        reserveProduct(reserveRequest, isInventoryServiceDown);

                        if (isInventoryServiceDown.get()) {
                            throw new OrderProcessingException("Inventory service is unavailable");
                        }

                        reservedOrders.add(reserveRequest);
                    });
            log.debug("OK reserved orders: {}", reservedOrders);
        } catch (OrderProcessingException e) {
            if (!isInventoryServiceDown.get()) {
                rollbackReserve(reservedOrders);
                log.warn("Failed to reserve products");
                throw e;
            }

            log.warn("Inventory service is unavailable, rolling back reserved products");
        }
    }

    private ProductDto getProduct(long productId, AtomicBoolean isProductServiceDown) {
        log.trace("getProduct {}", productId);
        RemoteCallResult<ProductDto> result = productAdapter.getProductById(productId);

        ProductDto productDto = switch (result) {
            case RemoteCallResult.Success<ProductDto> s -> s.value();
            case RemoteCallResult.BusinessFailure<ProductDto> bf -> {
                log.warn("Product with id {} not found: {}", productId, bf.message());
                throw new OrderProcessingException(String.format("Product with id %d not found", productId));
            }
            case RemoteCallResult.TechnicalFailure<ProductDto> tf -> {
                log.warn("Product service is unavailable: {}", tf.reason());
                isProductServiceDown.set(true);
                yield new ProductDto(
                        productId,
                        String.format("Product #%d (pending check)", productId),
                        "Manual check required",
                        BigDecimal.ZERO,
                        false
                );
            }
        };
        log.debug("OK {}", productDto);
        return productDto;
    }

    private void reserveProduct(ReserveRequest request, AtomicBoolean isInventoryServiceDown) {
        log.trace("reserveProduct {}", request);
        RemoteCallResult<Void> result = inventoryAdapter.reserveStock(request);
        switch (result) {
            case RemoteCallResult.Success<Void> s -> {
                log.debug("OK {}", request);
            }
            case RemoteCallResult.BusinessFailure<?> bf -> {
                log.warn("Business failure during reservation: {}", bf.message());
                throw new OrderProcessingException("Reservation error: " + bf.message());
            }
            case RemoteCallResult.TechnicalFailure<?> tf -> {
                log.warn("Technical failure during reservation: {}", tf.reason());
                isInventoryServiceDown.set(true);
            }
        }
    }

    private void rollbackReserve(List<ReserveRequest> reservedOrders) {
        log.trace("rollbackReserve {}", reservedOrders);
        reservedOrders.stream()
                .map(reserveResponse -> new ReleaseRequest(
                        reserveResponse.productId(),
                        reserveResponse.quantity()
                ))
                .forEach(this::releaseProduct);
        log.debug("OK rolled back {}", reservedOrders);
    }

    private void releaseProduct(ReleaseRequest request) {
        log.trace("releaseProduct {}", request);
        RemoteCallResult<Void> result = inventoryAdapter.releaseStock(request);
        switch (result) {
            case RemoteCallResult.Success<Void> s -> {
                log.debug("OK {}", request);
            }
            case RemoteCallResult.BusinessFailure<?> bf -> {
                log.warn("Business failure during release: {}", bf.message());
            }
            case RemoteCallResult.TechnicalFailure<?> tf -> {
                log.warn("Technical failure during release: {}", tf.reason());
            }
        }
    }

    private ProductDto withDescription(ProductDto product, String newDescription) {
        return new ProductDto(
                product.id(),
                product.name(),
                newDescription,
                product.price(),
                product.active()
        );
    }
}