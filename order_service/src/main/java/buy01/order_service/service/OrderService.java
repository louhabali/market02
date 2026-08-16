package buy01.order_service.service;

import buy01.order_service.dto.CreateOrderRequest;
import buy01.order_service.event.OrderCreatedEvent;
import buy01.order_service.event.OrderEventPublisher;
import buy01.order_service.exception.OrderNotFoundException;
import buy01.order_service.model.Order;
import buy01.order_service.model.OrderItem;
import buy01.order_service.model.OrderStatus;
import buy01.order_service.model.ShippingAddress;
import buy01.order_service.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public Order createOrder(String customerId, CreateOrderRequest request) {
        List<OrderItem> orderItems = request.getItems().stream()
                .map(dto -> OrderItem.builder()
                        .productId(dto.getProductId())
                        .sellerId(dto.getSellerId())
                        .productName(dto.getProductName())
                        .priceAtPurchase(dto.getPrice())
                        .quantity(dto.getQuantity())
                        .build())
                .toList();

        BigDecimal totalAmount = orderItems.stream()
                .map(item -> item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ShippingAddress address = ShippingAddress.builder()
                .fullName(request.getShippingAddress().getFullName())
                .phone(request.getShippingAddress().getPhone())
                .streetAddress(request.getShippingAddress().getStreetAddress())
                .city(request.getShippingAddress().getCity())
                .postalCode(request.getShippingAddress().getPostalCode())
                .build();

        Order order = Order.builder()
                .customerId(customerId)
                .items(orderItems)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .paymentMethod("PAY_ON_DELIVERY")
                .shippingAddress(address)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        // Publish event to Kafka to trigger async processes (e.g. clear cart)
        orderEventPublisher.publishOrderCreated(
                OrderCreatedEvent.builder()
                        .orderId(savedOrder.getId())
                        .customerId(customerId)
                        .totalAmount(totalAmount)
                        .build()
        );

        return savedOrder;
    }

    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public List<Order> getCustomerOrders(String customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<Order> getSellerOrders(String sellerId) {
        return orderRepository.findBySellerId(sellerId);
    }

    public Order updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = getOrderById(orderId);
        order.setStatus(newStatus);
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }

    public Order cancelOrder(String orderId, String customerId) {
        Order order = getOrderById(orderId);
        if (!order.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("You can only cancel your own orders");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }
}