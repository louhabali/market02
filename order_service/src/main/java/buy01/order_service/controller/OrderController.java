package buy01.order_service.controller;

import buy01.order_service.dto.CreateOrderRequest;
import buy01.order_service.dto.CustomerInsightsResponse;
import buy01.order_service.dto.SellerAnalyticsResponse;
import buy01.order_service.dto.UpdateOrderStatusRequest;
import buy01.order_service.model.Order;
import buy01.order_service.service.OrderService;
import org.springframework.data.domain.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestHeader("X-User-Id") String customerId,
            @Valid @RequestBody CreateOrderRequest request) {
        Order createdOrder = orderService.createOrder(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<Page<Order>> getCustomerOrders(
            @RequestHeader("X-User-Id") String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getCustomerOrders(customerId, page, size));
    }

    @GetMapping("/seller")
    public ResponseEntity<Page<Order>> getSellerOrders(
            @RequestHeader("X-User-Id") String sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getSellerOrders(sellerId, page, size));
    }

    @GetMapping("/my-insights")
    public ResponseEntity<CustomerInsightsResponse> getCustomerInsights(@RequestHeader("X-User-Id") String customerId) {
        return ResponseEntity.ok(orderService.getCustomerInsights(customerId));
    }

    @GetMapping("/seller/analytics")
    public ResponseEntity<SellerAnalyticsResponse> getSellerAnalytics(
            @RequestHeader("X-User-Id") String sellerId,
            @RequestHeader("X-Role") String userRole) {
        if (!"SELLER".equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(orderService.getSellerAnalytics(sellerId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request.getStatus()));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable String orderId,
            @RequestHeader("X-User-Id") String customerId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, customerId));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable String orderId,
            @RequestHeader("X-User-Id") String customerId) {
        orderService.deleteOrder(orderId, customerId);
        return ResponseEntity.noContent().build();
    }
}