package ru.yandex.practicum.order.dto;

public enum OrderStatus {
    CREATED,
    CONFIRMED,
    PENDING_CONFIRMATION,
    PAID,
    CLOSED
}