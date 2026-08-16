package buy01.order_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;
    private String customerId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String paymentMethod; // Value: "PAY_ON_DELIVERY"
    private ShippingAddress shippingAddress;
    private Instant createdAt;
    private Instant updatedAt;
}