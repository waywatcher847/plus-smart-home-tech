package ru.yandex.practicum.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.order.dto.*;
import ru.yandex.practicum.order.entity.*;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.feign.ProductDto;
import ru.yandex.practicum.order.repository.OrderRepository;
import ru.yandex.practicum.order.service.OrderServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTests {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;


    private Order buildOrder(long id, String email, BigDecimal totalPrice) {
        OrderItem item = OrderItem.builder()
                .id(id * 100)
                .productId(100L)
                .productName("Test")
                .quantity(1)
                .price(BigDecimal.valueOf(500.00))
                .build();

        Order order = Order.builder()
                .id(id)
                .customerName("aaa")
                .customerEmail(email)
                .totalPrice(totalPrice)
                .build();

        item.setOrder(order);
        order.getItems().add(item);

        return order;
    }

    @Test
    void getOrders_returnAsList() {
        Order order1 = buildOrder(1L, "asdasd1@asdasd.com", BigDecimal.valueOf(500.00));
        Order order2 = buildOrder(2L, "asdasd2@asdasd.com", BigDecimal.valueOf(500.00));

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

        List<OrderDto> result = orderService.getAllOrders();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
        verify(orderRepository).findAll();
    }

    @Test
    void getOrders_empty() {
        when(orderRepository.findAll()).thenReturn(List.of());

        List<OrderDto> result = orderService.getAllOrders();

        assertThat(result).isEmpty();
        verify(orderRepository).findAll();
    }

    @Test
    void saveOrder_ok() {
        OrderItemRequest itemRequest = new OrderItemRequest(100L, 2);

        CreateOrderRequest request = new CreateOrderRequest(
                "asdasd@asdasd.com", "aaa", List.of(itemRequest));

        ProductDto productDto = new ProductDto(
                100L, "asdasd", "zxczxc", new BigDecimal("9999.00"), true);
        Map<Long, ProductDto> productsMap = Map.of(100L, productDto);

        Order savedOrder = buildOrder(1L, "asdasd@asdasd.com", new BigDecimal("29998.00"));
        savedOrder.getItems().clear();
        OrderItem item = OrderItem.builder()
                .productId(100L)
                .productName("asdasd")
                .quantity(2)
                .price(new BigDecimal("9999.00"))
                .build();
        item.setOrder(savedOrder);
        savedOrder.getItems().add(item);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderDto result = orderService.saveOrder(request, productsMap);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).productName()).isEqualTo("asdasd");
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
        assertThat(result.totalPrice()).isEqualByComparingTo(new BigDecimal("29998.00"));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void saveOrder_twoItems_newPrice() {
        OrderItemRequest item1 = new OrderItemRequest(100L, 3);
        OrderItemRequest item2 = new OrderItemRequest(200L, 1);

        CreateOrderRequest request = new CreateOrderRequest(
                "asdasd@asdasd.com", "aaa", List.of(item1, item2));

        ProductDto product1 = new ProductDto(
                100L, "vvv", "zxczxc", new BigDecimal("500.00"), true);
        ProductDto product2 = new ProductDto(
                200L, "yyy", "zxczxc", new BigDecimal("50000.00"), true);
        Map<Long, ProductDto> productsMap = Map.of(100L, product1, 200L, product2);

        Order savedOrder = buildOrder(5L, "asdasd@asdasd.com", new BigDecimal("51500.00"));
        savedOrder.getItems().clear();

        OrderItem itemA = OrderItem.builder()
                .productId(100L).productName("vvv").quantity(3)
                .price(new BigDecimal("500.00")).build();
        itemA.setOrder(savedOrder);
        savedOrder.getItems().add(itemA);

        OrderItem itemB = OrderItem.builder()
                .productId(200L).productName("yyy").quantity(1)
                .price(new BigDecimal("50000.00")).build();
        itemB.setOrder(savedOrder);
        savedOrder.getItems().add(itemB);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderDto result = orderService.saveOrder(request, productsMap);

        assertThat(result.totalPrice()).isEqualByComparingTo(new BigDecimal("51500.00"));
        assertThat(result.items()).hasSize(2);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void saveOrder_setsOrderItemsRelation() {
        OrderItemRequest itemRequest = new OrderItemRequest(100L, 1);

        CreateOrderRequest request = new CreateOrderRequest(
                "asdasd@asdasd.com", "aaa", List.of(itemRequest));

        ProductDto productDto = new ProductDto(
                100L, "nnn", "zxczxc", new BigDecimal("100.00"), true);
        Map<Long, ProductDto> productsMap = Map.of(100L, productDto);

        Order savedOrder = buildOrder(1L, "asdasd@asdasd.com", new BigDecimal("100.00"));
        savedOrder.getItems().clear();

        OrderItem item = OrderItem.builder()
                .productId(100L)
                .productName("nnn")
                .quantity(1)
                .price(new BigDecimal("100.00"))
                .build();
        item.setOrder(savedOrder);
        savedOrder.getItems().add(item);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        orderService.saveOrder(request, productsMap);

        verify(orderRepository).save(argThat(order -> {
            List<OrderItem> items = order.getItems();
            return items.size() == 1
                    && items.get(0).getOrder() != null
                    && items.get(0).getOrder() == order;
        }));
    }

    @Test
    void getOrderById_success() {
        Order order = buildOrder(10L, "asdasd@asdasd.com", BigDecimal.valueOf(500.00));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        OrderDto result = orderService.getOrderById(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).productName()).isEqualTo("Test");
        verify(orderRepository).findById(10L);
    }

    @Test
    void getOrderById_throwsWhenNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> orderService.getOrderById(999L));

        verify(orderRepository).findById(999L);
    }

    @Test
    void getCustomersOrders_returnsList() {
        Order order1 = buildOrder(1L, "asdasd@asdasd.com", BigDecimal.valueOf(500.00));
        Order order2 = buildOrder(2L, "asdasd@asdasd.com", BigDecimal.valueOf(500.00));

        when(orderRepository.findByCustomerEmail("asdasd@asdasd.com"))
                .thenReturn(List.of(order1, order2));

        List<OrderDto> result = orderService.getCustomersOrders("asdasd@asdasd.com");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
        verify(orderRepository).findByCustomerEmail("asdasd@asdasd.com");
    }

    @Test
    void getCustomersOrders_emptyList() {
        when(orderRepository.findByCustomerEmail("zxczxc@zxczxc.com"))
                .thenReturn(List.of());

        List<OrderDto> result = orderService.getCustomersOrders("zxczxc@zxczxc.com");

        assertThat(result).isEmpty();
        verify(orderRepository).findByCustomerEmail("zxczxc@zxczxc.com");
    }
}