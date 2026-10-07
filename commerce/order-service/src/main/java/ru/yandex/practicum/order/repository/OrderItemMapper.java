package ru.yandex.practicum.order.repository;

import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.OrderItem;

public class OrderItemMapper {

    public static OrderItem toOrderItem(OrderItemRequest request) {
        return OrderItem.builder().productId(request.productId()).productName(request.productName()).quantity(request.quantity()).price(request.price()).build();
    }

    public static OrderItemDto toOrderItemDto(OrderItem orderItem) {
        return OrderItemDto.builder().id(orderItem.getId()).productId(orderItem.getProductId()).productName(orderItem.getProductName()).quantity(orderItem.getQuantity()).price(orderItem.getPrice()).build();
    }
}
