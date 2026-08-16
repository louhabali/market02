package buy01.order_service.controller;

import buy01.order_service.dto.CreateOrderRequest;
import buy01.order_service.dto.UpdateOrderStatusRequest;
import buy01.order_service.model.Order;
import buy01.order_service.service.OrderService;
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
    public ResponseEntity<List<Order>> getCustomerOrders(@RequestHeader("X-User-Id") String customerId) {
        return ResponseEntity.ok(orderService.getCustomerOrders(customerId));
    }

    @GetMapping("/seller")
    public ResponseEntity<List<Order>> getSellerOrders(@RequestHeader("X-User-Id") String sellerId) {
        return ResponseEntity.ok(orderService.getSellerOrders(sellerId));
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
}