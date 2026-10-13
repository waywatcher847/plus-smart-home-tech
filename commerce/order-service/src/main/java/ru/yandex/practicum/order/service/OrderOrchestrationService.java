package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.feign.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderOrchestrationService {
    private final OrderService orderService;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    public OrderDto createOrder(CreateOrderRequest request) {
        log.trace("createOrder {}",  request);

        Map<Long, ProductDto> productsMap = getUniqueProducts(request);
        log.debug("productsMap {}", productsMap);

        List<Long> nonActiveProducts = findNonActiveProducts(productsMap);

        if (!nonActiveProducts.isEmpty()) {
            String ids = nonActiveProducts.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            log.warn("id: {} nonActive", ids);
            throw new OrderProcessingException(String.format("id: %s nonActive", ids));
        }

        reserveProducts(request);

        return orderService.saveOrder(request, productsMap);
    }

    private Map<Long, ProductDto> getUniqueProducts(CreateOrderRequest request) {
        log.trace("getUniqueProducts {}",  request);
        return request.items().stream()
                .map(OrderItemRequest::productId)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::getProduct
                ));
    }

    private List<Long> findNonActiveProducts(Map<Long, ProductDto> productsMap) {
        log.trace("findNonActiveProducts {}",  productsMap);
        return productsMap.values().stream()
                .filter(product -> !product.active())
                .map(ProductDto::id)
                .toList();
    }

    private void reserveProducts(CreateOrderRequest request) {
        log.trace("reserveProducts {}",  request);
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
                        reserveProduct(reserveRequest);
                        reservedOrders.add(reserveRequest);
                    });
        } catch (FeignException e) {
            rollbackReserve(reservedOrders);
            log.warn("rollbackReserve");
            throw new OrderProcessingException("rollbackReserve");
        }

    }

    private ProductDto getProduct(long productId) {
        log.trace("getProduct {}", productId);
        try {
            return productClient.getProductById(productId);
        } catch (FeignException.NotFound e) {
            log.warn("Not Found {}", productId);
            throw new OrderProcessingException(String.format("id %d Not Found", productId));
        }
    }

    private void reserveProduct(ReserveRequest request) {
        log.trace("reserveProduct {}", request);

        try {
            inventoryClient.reserveStock(request);
        } catch (FeignException.ServiceUnavailable e) {
            log.warn("ServiceUnavailable");
            throw new OrderProcessingException("ServiceUnavailable");
        }
    }

    private void rollbackReserve(List<ReserveRequest> reservedOrders) {
        reservedOrders.stream()
                .map(reserveResponse -> new ReleaseRequest(
                        reserveResponse.productId(),
                        reserveResponse.quantity()
                ))
                .forEach(this::releaseProduct);
    }

    private void releaseProduct(ReleaseRequest request) {
        try {
            inventoryClient.releaseStock(request);
        } catch (FeignException.NotFound e) {
            log.warn("releaseStock NotFound");
            throw new OrderProcessingException("releaseStock NotFound");
        }
    }
}