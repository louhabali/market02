package buy01.cart_service.service;

import buy01.cart_service.dto.AddToCartRequest;
import buy01.cart_service.model.CartItem;
import buy01.cart_service.model.ShoppingCart;
import buy01.cart_service.model.ShoppingCartDocument;
import buy01.cart_service.repo.CartMongoRepository;
import buy01.cart_service.repo.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartMongoRepository cartMongoRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String ACTIVE_CARTS_KEY = "active_carts";

    public ShoppingCart getCart(String userId) {
        Optional<ShoppingCart> redisCart = cartRepository.findById(userId);
        if (redisCart.isPresent()) {
            log.debug("Cart found in Redis for user: {}", userId);
            return redisCart.get();
        }

        Optional<ShoppingCartDocument> mongoCart = cartMongoRepository.findById(userId);
        if (mongoCart.isPresent()) {
            ShoppingCartDocument doc = mongoCart.get();
            if (doc.getItems() != null && !doc.getItems().isEmpty()) {
                log.debug("Cart restored from MongoDB for user: {}", userId);
                ShoppingCart cart = convertToShoppingCart(doc);
                cartRepository.save(cart);
                redisTemplate.opsForSet().add(ACTIVE_CARTS_KEY, userId);
                return cart;
            } else {
                cartMongoRepository.deleteById(userId);
            }
        }

        return ShoppingCart.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .build();
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
                    .imageUrl(request.getImageUrl())
                    .price(request.getPrice())
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
        }

        cartRepository.save(cart);
        redisTemplate.opsForSet().add(ACTIVE_CARTS_KEY, userId);
        
        log.debug("Cart saved to Redis for user: {} (TTL 7 days)", userId);
        return cart;
    }

    public ShoppingCart removeItemFromCart(String userId, String productId) {
        ShoppingCart cart = getCart(userId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));

        cartRepository.save(cart);

        if (cart.getItems().isEmpty()) {
            cartMongoRepository.deleteById(userId);
            redisTemplate.opsForSet().remove(ACTIVE_CARTS_KEY, userId);
            log.debug("Empty cart removed from MongoDB for user: {}", userId);
        }

        return cart;
    }

    public void clearCart(String userId) {
        cartRepository.deleteById(userId);
        cartMongoRepository.deleteById(userId);
        redisTemplate.opsForSet().remove(ACTIVE_CARTS_KEY, userId);
        log.info("Cart cleared for user: {}", userId);
    }

    // Scheduled job: Backup active carts to MongoDB daily at 1 AM
    @Scheduled(cron = "0 0 1 * * *")
    public void backupActiveCartsToMongoDB() {
        log.info("Starting backup of active carts to MongoDB...");
        
        Set<String> activeUsers = redisTemplate.opsForSet().members(ACTIVE_CARTS_KEY);
        
        if (activeUsers.isEmpty()) {
            log.info("No active carts to backup");
            return;
        }

        int backedUpCount = 0;
        
        for (String userId : activeUsers) {
            try {
                Optional<ShoppingCart> cartOpt = cartRepository.findById(userId);
                if (cartOpt.isPresent()) {
                    ShoppingCart cart = cartOpt.get();
                    if (cart.getItems() != null && !cart.getItems().isEmpty()) {
                        ShoppingCartDocument doc = ShoppingCartDocument.builder()
                                .userId(cart.getUserId())
                                .items(new ArrayList<>(cart.getItems()))
                                .totalAmount(cart.getTotalAmount())
                                .expiresAt(Instant.now().plusSeconds(604800).getEpochSecond())
                                .build();
                        cartMongoRepository.save(doc);
                        backedUpCount++;
                        log.debug("Backed up cart for user: {}", userId);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to backup cart for user: {}", userId, e);
            }
        }
        
        log.info("Backed up {} carts to MongoDB", backedUpCount);
    }

    // Scheduled job: Cleanup expired MongoDB carts at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredMongoCarts() {
        long currentTime = Instant.now().getEpochSecond();
        cartMongoRepository.deleteByExpiresAtBefore(currentTime);
        log.info("Cleaned up expired carts from MongoDB");
    }

    private ShoppingCart convertToShoppingCart(ShoppingCartDocument doc) {
        return ShoppingCart.builder()
                .userId(doc.getUserId())
                .items(new ArrayList<>(doc.getItems()))
                .build();
    }
}