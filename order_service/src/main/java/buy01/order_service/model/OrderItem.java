package buy01.order_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {
    private String productId;
    private String sellerId;
    private String productName;
    private BigDecimal priceAtPurchase;
    private Integer quantity;
}