package ru.yandex.practicum.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.service.OrderService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ControllerConstants.URL_API + ControllerConstants.URL_ORDERS)
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    public List<OrderDto> getAllOrders() {
        return orderService.getAllOrders();
    }
    @GetMapping(ControllerConstants.ID_PARAM)
    public OrderDto getOrderById(@PathVariable(name = ControllerConstants.ID) long orderId) {
        return orderService.getOrderById(orderId);
    }

    @GetMapping(ControllerConstants.URL_BY_EMAIL)
    public List<OrderDto> getCustomersOrders(@RequestParam String email) {
        return orderService.getCustomersOrders(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

}
