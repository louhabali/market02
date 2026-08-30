package buy01.order_service.service;

import buy01.order_service.dto.CreateOrderRequest;
import buy01.order_service.dto.CustomerInsightsResponse;
import buy01.order_service.dto.SellerAnalyticsResponse;
import buy01.order_service.event.OrderCreatedEvent;
import buy01.order_service.event.OrderEventPublisher;
import buy01.order_service.exception.OrderNotFoundException;
import buy01.order_service.model.Order;
import buy01.order_service.model.OrderItem;
import buy01.order_service.model.OrderStatus;
import buy01.order_service.model.ShippingAddress;
import buy01.order_service.repo.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final ProductInventoryClient productInventoryClient;

    public Order createOrder(String customerId, CreateOrderRequest request) {
        for (OrderItem item : request.getItems().stream()
                .map(dto -> OrderItem.builder()
                        .productId(dto.getProductId())
                        .sellerId(dto.getSellerId())
                        .productName(dto.getProductName())
                        .category(dto.getCategory())
                        .priceAtPurchase(dto.getPrice())
                        .quantity(dto.getQuantity())
                        .build())
                .toList()) {
            int available = productInventoryClient.getAvailableQuantity(item.getProductId());
            if (item.getQuantity() > available) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Only " + available + " item(s) available in stock for product " + item.getProductId());
            }
        }

        List<OrderItem> orderItems = request.getItems().stream()
                .map(dto -> OrderItem.builder()
                        .productId(dto.getProductId())
                        .sellerId(dto.getSellerId())
                        .productName(dto.getProductName())
                        .category(dto.getCategory())
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

        for (OrderItem item : savedOrder.getItems()) {
            productInventoryClient.decrementStock(item.getProductId(), item.getQuantity());
        }

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
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(0, 20)).getContent();
    }

    public Page<Order> getCustomerOrders(String customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
    }

    public List<Order> getSellerOrders(String sellerId) {
        return orderRepository.findBySellerId(sellerId, PageRequest.of(0, 20)).getContent();
    }

    public Page<Order> getSellerOrders(String sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findBySellerId(sellerId, pageable);
    }

    public CustomerInsightsResponse getCustomerInsights(String customerId) {
        List<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        List<Order> completedOrders = orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .toList();

        BigDecimal totalSpent = completedOrders.stream()
                .map(Order::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Integer> categoryTotals = new HashMap<>();
        for (Order order : completedOrders) {
            for (OrderItem item : order.getItems() == null ? List.<OrderItem>of() : order.getItems()) {
                String category = item.getCategory() == null || item.getCategory().isBlank()
                        ? "Uncategorized"
                        : item.getCategory();
                int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                categoryTotals.put(category, categoryTotals.getOrDefault(category, 0) + quantity);
            }
        }

        String topCategory = categoryTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return CustomerInsightsResponse.builder()
                .totalSpent(totalSpent)
                .totalOrders(orders.size())
                .topCategory(topCategory)
                .build();
    }

    public SellerAnalyticsResponse getSellerAnalytics(String sellerId) {
        List<Order> orders = getSellerOrders(sellerId, 0, Integer.MAX_VALUE).getContent().stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .toList();
        Map<String, SellerAnalyticsResponse.TopProduct> products = new HashMap<>();

        for (Order order : orders) {
            for (OrderItem item : order.getItems() == null ? List.<OrderItem>of() : order.getItems()) {
                if (!sellerId.equals(item.getSellerId())) {
                    continue;
                }
                SellerAnalyticsResponse.TopProduct current = products.get(item.getProductId());
                BigDecimal revenue = item.getPriceAtPurchase()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                if (current == null) {
                    products.put(item.getProductId(), SellerAnalyticsResponse.TopProduct.builder()
                            .productId(item.getProductId())
                            .name(item.getProductName())
                            .unitsSold(item.getQuantity())
                            .revenue(revenue)
                            .build());
                } else {
                    current.setUnitsSold(current.getUnitsSold() + item.getQuantity());
                    current.setRevenue(current.getRevenue().add(revenue));
                }
            }
        }

        List<SellerAnalyticsResponse.TopProduct> topProducts = products.values().stream()
                .sorted((first, second) -> Long.compare(second.getUnitsSold(), first.getUnitsSold()))
                .toList();
        BigDecimal totalRevenue = topProducts.stream()
                .map(SellerAnalyticsResponse.TopProduct::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalUnitsSold = topProducts.stream()
                .mapToLong(SellerAnalyticsResponse.TopProduct::getUnitsSold)
                .sum();

        return SellerAnalyticsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalUnitsSold(totalUnitsSold)
                .totalOrders(orders.size())
                .topProducts(topProducts)
                .build();
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