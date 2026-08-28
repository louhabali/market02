package buy01.cart_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {
    private String productId;
    private String sellerId;
    private String productName;
    private String category;
    private String imageUrl;
    private BigDecimal price;
    private Integer quantity;
}