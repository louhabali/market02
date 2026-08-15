package buy01.cart_service.event;

import buy01.cart_service.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final CartService cartService;

    @KafkaListener(topics = "order-created-topic", groupId = "cart-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order created event for orderId: {} and customerId: {}", 
                event.getOrderId(), event.getCustomerId());
        
        // Asynchronously clear the user's active Redis cart after successful checkout
        cartService.clearCart(event.getCustomerId());
        log.info("Cleared active cart for customerId: {}", event.getCustomerId());
    }
}