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

    private static final long SEVEN_DAYS_SECONDS = 604800L;

    
    public ShoppingCart getCart(String userId) {

        // Try Redis first
        Optional<ShoppingCart> redisCart = cartRepository.findById(userId);

        if (redisCart.isPresent()) {
            log.debug("Cart found in Redis for user: {}", userId);
            return redisCart.get();
        }

        Optional<ShoppingCartDocument> mongoCart = cartMongoRepository.findById(userId);

        if (mongoCart.isPresent()) {

            ShoppingCartDocument doc = mongoCart.get();

            if (doc.getItems() != null && !doc.getItems().isEmpty()) {

                log.info("Cart restored from MongoDB for user: {}",userId);

                ShoppingCart cart = convertToShoppingCart(doc);

                cartRepository.save(cart);

                // Mark as active
                redisTemplate.opsForSet().add(ACTIVE_CARTS_KEY, userId);

                return cart;
            }

            cartMongoRepository.deleteById(userId);
        }

        return ShoppingCart.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .createdAt(null)
                .build();
    }

    public ShoppingCart addItemToCart(
            String userId,
            AddToCartRequest request) {

        ShoppingCart cart = getCart(userId);

        // If this is a completely new cart,
        // initialize creation time.
        if (cart.getCreatedAt() == null) {
            cart.setCreatedAt(
                    Instant.now().getEpochSecond()
            );
        }

        CartItem existingItem = cart.getItems()
                .stream()
                .filter(item ->
                        item.getProductId()
                                .equals(request.getProductId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {

            existingItem.setQuantity(
                    existingItem.getQuantity()
                            + request.getQuantity()
            );

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

        log.info("Cart saved ONLY to Redis for user: {}",userId);

        return cart;
    }

    public ShoppingCart removeItemFromCart(
            String userId,
            String productId) {

        ShoppingCart cart = getCart(userId);

        cart.getItems()
                .removeIf(item ->
                        item.getProductId()
                                .equals(productId));

        if (cart.getItems().isEmpty()) {

            cartRepository.deleteById(userId);
            cartMongoRepository.deleteById(userId);

            redisTemplate.opsForSet().remove(ACTIVE_CARTS_KEY, userId);

            log.info("Empty cart deleted for user: {}",userId);

            return cart;
        }

        /** Save only to Redis.*/

        cartRepository.save(cart);

        return cart;
    }

    public void clearCart(String userId) {

        cartRepository.deleteById(userId);

        cartMongoRepository.deleteById(userId);

        redisTemplate.opsForSet().remove(ACTIVE_CARTS_KEY, userId);

        log.info("Cart completely cleared for user: {}",userId);
    }

    /**
     * Backup carts to MongoDB AFTER 7 DAYS.*/
    // @Scheduled(cron = "0 0 * * * *")
    @Scheduled(cron = "0 0 1 * * *")
    public void backupOldActiveCartsToMongoDB() {

        log.info(
                "Starting 7-day cart backup process..."
        );

        Set<String> activeUsers =
                redisTemplate.opsForSet()
                        .members(ACTIVE_CARTS_KEY);

        if (activeUsers == null || activeUsers.isEmpty()) {

            log.info("No active carts found");

            return;
        }

        long now = Instant.now().getEpochSecond();

        int backedUpCount = 0;

        for (String userId : activeUsers) {

            try {

                Optional<ShoppingCart> cartOpt =
                        cartRepository.findById(userId);

                if (cartOpt.isEmpty()) {

                    redisTemplate.opsForSet()
                            .remove(ACTIVE_CARTS_KEY, userId);

                    continue;
                }

                ShoppingCart cart = cartOpt.get();

                if (cart.getItems() == null ||
                        cart.getItems().isEmpty()) {

                    redisTemplate.opsForSet()
                            .remove(ACTIVE_CARTS_KEY, userId);

                    continue;
                }

                if (cart.getCreatedAt() == null) {

                    log.warn(
                            "Cart {} has no createdAt, skipping backup",
                            userId
                    );

                    continue;
                }

                long cartAge = now - cart.getCreatedAt();

                if (cartAge < SEVEN_DAYS_SECONDS) {

                    log.debug("Cart {} is only {} seconds old. " + "Waiting until 7 days.", userId, cartAge);

                    continue;
                }
                
                Optional<ShoppingCartDocument> existingMongoCart = cartMongoRepository.findById(userId);

                    if (existingMongoCart.isPresent()) {
                        continue;
                    }

                ShoppingCartDocument doc = ShoppingCartDocument.builder()
                                .userId(cart.getUserId())
                                .items(new ArrayList<>(
                                        cart.getItems()))
                                .totalAmount(
                                        cart.getTotalAmount())
                                .createdAt(
                                        cart.getCreatedAt())
                                .expiresAt(
                                        now + SEVEN_DAYS_SECONDS)
                                .build();

                cartMongoRepository.save(doc);

                backedUpCount++;

                log.info("Cart backed up to MongoDB after 7 days: {}", userId);

            } catch (Exception e) {

                log.error("Failed to backup cart for user: {}", userId, e);
            }
        }

        log.info("7-day backup finished. {} carts backed up.", backedUpCount);
    }

    /** Delete expired MongoDB carts.*/
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredMongoCarts() {

        long currentTime = Instant.now().getEpochSecond();
        
        cartMongoRepository.deleteByExpiresAtBefore(currentTime);

        log.info("Expired MongoDB carts cleaned up");
    }

    /** Convert Mongo document to Redis cart.*/
    private ShoppingCart convertToShoppingCart(
            ShoppingCartDocument doc) {

        return ShoppingCart.builder()
                .userId(doc.getUserId())
                .items(new ArrayList<>(doc.getItems()))
                .createdAt(doc.getCreatedAt())
                .build();
    }
}