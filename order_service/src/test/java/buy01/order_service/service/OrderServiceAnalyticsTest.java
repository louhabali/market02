package buy01.order_service.service;

import buy01.order_service.dto.CreateOrderRequest;
import buy01.order_service.dto.CustomerInsightsResponse;
import buy01.order_service.dto.OrderItemDto;
import buy01.order_service.dto.SellerAnalyticsResponse;
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
import org.springframework.data.domain.Page;
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

        @Test
        void shouldReturnOrderByIdAndHandleMissingOrders() {
                Order order = Order.builder().id("o-7").customerId("user-123").status(OrderStatus.PENDING).build();
                when(orderRepository.findById("o-7")).thenReturn(Optional.of(order));

                assertThat(orderService.getOrderById("o-7")).isEqualTo(order);
                when(orderRepository.findById("missing")).thenReturn(Optional.empty());
                assertThrows(RuntimeException.class, () -> orderService.getOrderById("missing"));
        }

        @Test
        void shouldReturnPagedCustomerAndSellerOrders() {
                Order customerOrder = Order.builder()
                                .id("o-1")
                                .customerId("user-123")
                                .status(OrderStatus.PENDING)
                                .items(List.of(OrderItem.builder().productId("p1").sellerId("s1").productName("Jacket").category("Outerwear").priceAtPurchase(new BigDecimal("50.00")).quantity(1).build()))
                                .totalAmount(new BigDecimal("50.00"))
                                .build();
                Order sellerOrder = Order.builder()
                                .id("o-2")
                                .customerId("buyer-1")
                                .status(OrderStatus.DELIVERED)
                                .items(List.of(OrderItem.builder().productId("p1").sellerId("s1").productName("Jacket").category("Outerwear").priceAtPurchase(new BigDecimal("50.00")).quantity(2).build()))
                                .totalAmount(new BigDecimal("100.00"))
                                .build();

                when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(eq("user-123"), any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(customerOrder)));
                when(orderRepository.findBySellerId(eq("s1"), any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(sellerOrder)));

                assertThat(orderService.getCustomerOrders("user-123")).hasSize(1);
                Page<Order> customerPage = orderService.getCustomerOrders("user-123", 0, 10);
                assertThat(customerPage.getContent()).hasSize(1);
                Page<Order> sellerPage = orderService.getSellerOrders("s1", 0, 10);
                assertThat(sellerPage.getContent()).hasSize(1);
        }

        @Test
        void shouldComputeSellerAnalyticsAndValidateCancelGuards() {
                Order order = Order.builder()
                                .id("o-5")
                                .customerId("user-123")
                                .status(OrderStatus.DELIVERED)
                                .items(List.of(
                                                OrderItem.builder().productId("p1").sellerId("s1").productName("Jacket").category("Outerwear").priceAtPurchase(new BigDecimal("40.00")).quantity(2).build(),
                                                OrderItem.builder().productId("p2").sellerId("s2").productName("Sneakers").category("Footwear").priceAtPurchase(new BigDecimal("30.00")).quantity(1).build()))
                                .totalAmount(new BigDecimal("110.00"))
                                .build();

                when(orderRepository.findBySellerId(eq("s1"), any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(order)));

                SellerAnalyticsResponse analytics = orderService.getSellerAnalytics("s1");
                assertThat(analytics.getTotalRevenue()).isEqualByComparingTo("80.00");
                assertThat(analytics.getTotalUnitsSold()).isEqualTo(2);
                assertThat(analytics.getTotalOrders()).isEqualTo(1);

                Order pendingOrder = Order.builder().id("o-6").customerId("user-123").status(OrderStatus.PENDING).build();
                when(orderRepository.findById("o-6")).thenReturn(Optional.of(pendingOrder));
                when(orderRepository.save(pendingOrder)).thenReturn(pendingOrder);
                assertThat(orderService.updateOrderStatus("o-6", OrderStatus.CONFIRMED).getStatus()).isEqualTo(OrderStatus.CONFIRMED);

                when(orderRepository.findById("o-9")).thenReturn(Optional.of(Order.builder().id("o-9").customerId("user-999").status(OrderStatus.PENDING).build()));
                assertThrows(IllegalArgumentException.class, () -> orderService.cancelOrder("o-9", "user-123"));

                when(orderRepository.findById("o-10")).thenReturn(Optional.of(Order.builder().id("o-10").customerId("user-123").status(OrderStatus.DELIVERED).build()));
                assertThrows(IllegalStateException.class, () -> orderService.cancelOrder("o-10", "user-123"));
        }
}
