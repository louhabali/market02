package buy01.order_service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private static final String TOPIC = "order-created-topic";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for orderId: {}", event.getOrderId());
        kafkaTemplate.send(TOPIC, event.getOrderId(), event);
    }
}