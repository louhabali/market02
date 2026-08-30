package buy01.order_service.service;

import buy01.order_service.dto.CreateOrderRequest;
import buy01.order_service.dto.CustomerInsightsResponse;
import buy01.order_service.dto.OrderItemDto;
import buy01.order_service.dto.ShippingAddressDto;
import buy01.order_service.event.OrderCreatedEvent;
import buy01.order_service.event.OrderEventPublisher;
import buy01.order_service.model.Order;
import buy01.order_service.model.OrderItem;
import buy01.order_service.model.OrderStatus;
import buy01.order_service.repo.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceAnalyticsTest {

        @Mock
        private OrderRepository orderRepository;

        @Mock
        private OrderEventPublisher orderEventPublisher;

        @Mock
        private ProductInventoryClient productInventoryClient;

        @InjectMocks
        private OrderService orderService;

        @Test
        void shouldAggregateCustomerInsightsFromOrders() {
                Order order1 = Order.builder()
                                .id("o-1")
                                .customerId("user-123")
                                .status(OrderStatus.DELIVERED)
                                .totalAmount(new BigDecimal("120.50"))
                                .items(List.of(
                                                OrderItem.builder().productId("p1").productName("Jacket")
                                                                .category("Outerwear").sellerId("s1")
                                                                .priceAtPurchase(new BigDecimal("50.00")).quantity(2)
                                                                .build(),
                                                OrderItem.builder().productId("p2").productName("Sneakers")
                                                                .category("Footwear").sellerId("s2")
                                                                .priceAtPurchase(new BigDecimal("20.50")).quantity(1)
                                                                .build()))
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                Order order2 = Order.builder()
                                .id("o-2")
                                .customerId("user-123")
                                .status(OrderStatus.CANCELLED)
                                .totalAmount(new BigDecimal("40.00"))
                                .items(List.of(
                                                OrderItem.builder().productId("p3").productName("Hoodie")
                                                                .category("Outerwear").sellerId("s3")
                                                                .priceAtPurchase(new BigDecimal("40.00")).quantity(1)
                                                                .build()))
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(eq("user-123"), any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(order1, order2)));
                CustomerInsightsResponse insights = orderService.getCustomerInsights("user-123");

                assertThat(insights.getTotalSpent()).isEqualByComparingTo("120.50");
                assertThat(insights.getTotalOrders()).isEqualTo(2);
                assertThat(insights.getTopCategory()).isEqualTo("Outerwear");
        }

        @Test
        void shouldCreateOrderWhenStockIsAvailable() {
                CreateOrderRequest request = CreateOrderRequest.builder()
                                .items(List.of(
                                                OrderItemDto.builder()
                                                                .productId("p1")
                                                                .sellerId("s1")
                                                                .productName("Jacket")
                                                                .category("Outerwear")
                                                                .price(new BigDecimal("50.00"))
                                                                .quantity(2)
                                                                .build()))
                                .shippingAddress(ShippingAddressDto.builder()
                                                .fullName("Test User")
                                                .phone("123456")
                                                .streetAddress("Main Street 1")
                                                .city("Paris")
                                                .postalCode("75000")
                                                .build())
                                .build();

                when(productInventoryClient.getAvailableQuantity("p1")).thenReturn(5);
                when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                        Order order = invocation.getArgument(0);
                        order.setId("o-100");
                        return order;
                });

                Order saved = orderService.createOrder("user-123", request);

                assertThat(saved.getId()).isEqualTo("o-100");
                assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
                assertThat(saved.getTotalAmount()).isEqualByComparingTo("100.00");
                verify(productInventoryClient).decrementStock("p1", 2);
                verify(orderEventPublisher).publishOrderCreated(any(OrderCreatedEvent.class));
        }

        @Test
        void shouldRejectOrderWhenStockIsInsufficient() {
                CreateOrderRequest request = CreateOrderRequest.builder()
                                .items(List.of(
                                                OrderItemDto.builder()
                                                                .productId("p1")
                                                                .sellerId("s1")
                                                                .productName("Jacket")
                                                                .category("Outerwear")
                                                                .price(new BigDecimal("50.00"))
                                                                .quantity(3)
                                                                .build()))
                                .shippingAddress(ShippingAddressDto.builder()
                                                .fullName("Test User")
                                                .phone("123456")
                                                .streetAddress("Main Street 1")
                                                .city("Paris")
                                                .postalCode("75000")
                                                .build())
                                .build();

                when(productInventoryClient.getAvailableQuantity("p1")).thenReturn(2);

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> orderService.createOrder("user-123", request));

                assertThat(ex.getStatusCode().value()).isEqualTo(400);
                assertThat(ex.getReason()).contains("Only 2 item(s)");
        }

        @Test
        void shouldLoadExistingOrderAndCancelIt() {
                Order order = Order.builder()
                                .id("o-2")
                                .customerId("user-123")
                                .status(OrderStatus.PENDING)
                                .totalAmount(new BigDecimal("25.00"))
                                .build();

                when(orderRepository.findById("o-2")).thenReturn(Optional.of(order));
                when(orderRepository.save(order)).thenReturn(order);

                Order result = orderService.cancelOrder("o-2", "user-123");

                assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
}
