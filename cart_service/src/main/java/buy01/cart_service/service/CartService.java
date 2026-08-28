package buy01.cart_service.service;

import buy01.cart_service.dto.AddToCartRequest;
import buy01.cart_service.dto.UpdateQuantityRequest;
import buy01.cart_service.model.CartItem;
import buy01.cart_service.model.ShoppingCart;
import buy01.cart_service.repo.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    public ShoppingCart getCart(String userId) {
        return cartRepository.findById(userId)
                .orElse(ShoppingCart.builder()
                        .userId(userId)
                        .items(new ArrayList<>())
                        .build());
    }

    public ShoppingCart addItemToCart(String userId, AddToCartRequest request) {
        ShoppingCart cart = getCart(userId);

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
        } else {
            CartItem newItem = CartItem.builder()
                    .productId(request.getProductId())
                    .sellerId(request.getSellerId())
                    .productName(request.getProductName())
                    .category(request.getCategory())
                    .imageUrl(request.getImageUrl())
                    .price(request.getPrice())
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
        }

        return cartRepository.save(cart);
    }

    public ShoppingCart removeItemFromCart(String userId, String productId) {
        ShoppingCart cart = getCart(userId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        return cartRepository.save(cart);
    }

    public ShoppingCart updateItemQuantity(String userId, UpdateQuantityRequest request) {
        ShoppingCart cart = getCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getProductId().equals(request.getProductId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Product not found in cart: " + request.getProductId()));

        item.setQuantity(request.getQuantity());
        return cartRepository.save(cart);
    }

    public void clearCart(String userId) {
        cartRepository.deleteById(userId);
    }
}