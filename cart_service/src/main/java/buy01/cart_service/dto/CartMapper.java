package buy01.cart_service.dto;

import buy01.cart_service.model.ShoppingCart;

public class CartMapper {

    public static CartResponse toResponse(ShoppingCart cart) {
        int itemSum = cart.getItems() == null ? 0 : 
            cart.getItems().stream().mapToInt(item -> item.getQuantity()).sum();

        return CartResponse.builder()
                .userId(cart.getUserId())
                .items(cart.getItems())
                .totalAmount(cart.getTotalAmount())
                .totalItems(itemSum)
                .build();
    }
}