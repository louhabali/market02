package buy01.cart_service.dto;

import buy01.cart_service.model.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartResponse {

    private String userId;
    private List<CartItem> items;
    private BigDecimal totalAmount;
    private Integer totalItems;
}