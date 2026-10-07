package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.*;
import ru.yandex.practicum.order.entity.*;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.repository.*;

import java.math.BigDecimal;
import java.util.List;

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
        return getOrderDtoList(orders);
    }

    @Override
    public OrderDto createOrder(CreateOrderRequest request) {
        log.trace("createOrder {}", request);

        BigDecimal totalPrice = request.items().stream()
                .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Order order = OrderMapper.toOrder(request, totalPrice);

        for (OrderItemRequest itemRequest : request.items()) {
            OrderItem orderItem = OrderItemMapper.toOrderItem(itemRequest);
            orderItem.setOrder(order);
            order.getItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        log.debug("OK {}", savedOrder);

        List<OrderItemDto> savedOrderItemList = getOrderItemDtoList(savedOrder);
        return OrderMapper.toOrderDto(savedOrder, savedOrderItemList);
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
        List<OrderItemDto> orderItemList = getOrderItemDtoList(order);
        return OrderMapper.toOrderDto(order, orderItemList);
    }

    @Override
    public List<OrderDto> getCustomersOrders(String email) {
        log.trace("getCustomersOrders {}", email);
        List<Order> orders = orderRepository.findByCustomerEmail(email);
        log.debug("OK {}", orders);
        return getOrderDtoList(orders);
    }

    private List<OrderDto> getOrderDtoList(List<Order> orders) {
        return orders.stream()
                .map(order -> {
                    List<OrderItemDto> orderItems = getOrderItemDtoList(order);
                    return OrderMapper.toOrderDto(order, orderItems);
                })
                .toList();
    }

    private List<OrderItemDto> getOrderItemDtoList(Order order) {
        return order.getItems().stream()
                .map(OrderItemMapper::toOrderItemDto)
                .toList();
    }
}
