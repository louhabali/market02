package buy01.order_service.service;

import buy01.order_service.dto.CustomerInsightsResponse;
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
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceAnalyticsTest {

        @Mock
        private OrderRepository orderRepository;

        @Mock
        private OrderEventPublisher orderEventPublisher;

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
                                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(order1, order2)));
                CustomerInsightsResponse insights = orderService.getCustomerInsights("user-123");

                assertThat(insights.getTotalSpent()).isEqualByComparingTo("120.50");
                assertThat(insights.getTotalOrders()).isEqualTo(2);
                assertThat(insights.getTopCategory()).isEqualTo("Outerwear");
        }
}
