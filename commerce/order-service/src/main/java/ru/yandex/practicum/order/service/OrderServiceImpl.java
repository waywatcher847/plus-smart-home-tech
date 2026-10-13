package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.*;
import ru.yandex.practicum.order.entity.*;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.feign.ProductDto;
import ru.yandex.practicum.order.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;

    @Override
    public List<OrderDto> getAllOrders() {
        log.trace("getAllOrders");
        List<Order> orders = orderRepository.findAll();
        log.debug("OK {}", orders);
        return buildOrderDtoList(orders);
    }

    @Override
    @Transactional
    public OrderDto saveOrder(CreateOrderRequest request, Map<Long, ProductDto> productsMap) {
        log.trace("Инициировано сохранение заказа");
        Order order = buildOrder(request, productsMap);
        Order savedOrder = orderRepository.save(order);
        log.debug("Сохранен заказ {}", savedOrder);
        List<OrderItemDto> savedOrderItemDtoList = buildOrderItemDtoList(savedOrder);
        return OrderMapper.toOrderDto(savedOrder, savedOrderItemDtoList);
    }

    @Override
    public OrderDto getOrderById(long orderId) {
        log.trace("getOrderById {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("id {} NotFound", orderId);
                    return new NotFoundException(String.format("id %d NotFound", orderId));
                });
        log.debug("OK {}", order);
        List<OrderItemDto> orderItemList = buildOrderItemDtoList(order);
        return OrderMapper.toOrderDto(order, orderItemList);
    }

    @Override
    public List<OrderDto> getCustomersOrders(String email) {
        log.trace("getCustomersOrders {}", email);
        List<Order> orders = orderRepository.findByCustomerEmail(email);
        log.debug("OK {}", orders);
        return buildOrderDtoList(orders);
    }

    private List<OrderDto> buildOrderDtoList(List<Order> orders) {
        return orders.stream()
                .map(order -> {
                    List<OrderItemDto> orderItems = buildOrderItemDtoList(order);
                    return OrderMapper.toOrderDto(order, orderItems);
                })
                .toList();
    }

    private List<OrderItemDto> buildOrderItemDtoList(Order order) {
        return order.getItems().stream()
                .map(OrderItemMapper::toOrderItemDto)
                .toList();
    }

    private Order buildOrder(CreateOrderRequest request, Map<Long, ProductDto> productsMap) {
        Order order = OrderMapper.toOrder(request);

        for (OrderItemRequest itemRequest : request.items()) {
            ProductDto productDto = productsMap.get(itemRequest.productId());
            OrderItem orderItem = OrderItemMapper.toOrderItem(itemRequest);
            orderItem.setProductName(productDto.name());
            orderItem.setPrice(productDto.price());
            order.getItems().add(orderItem);
        }

        BigDecimal totalPrice = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalPrice(totalPrice);

        return order;
    }
}
