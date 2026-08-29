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
import java.util.List;
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


    /**Redis*/
    public ShoppingCart getCart(String userId) {

        List<CartItem> allItems = new ArrayList<>();


        Optional<ShoppingCart> redisCartOpt =
                cartRepository.findById(userId);

        if (redisCartOpt.isPresent() && redisCartOpt.get().getItems() != null) {

            allItems.addAll(redisCartOpt.get().getItems());

            log.debug("Found {} active products in Redis for user {}", redisCartOpt.get().getItems().size(), userId);
        }

        // Produits permanents dans MongoDB

        Optional<ShoppingCartDocument> mongoCartOpt =
                cartMongoRepository.findById(userId);

        if (mongoCartOpt.isPresent() && mongoCartOpt.get().getItems() != null) {

            allItems.addAll(mongoCartOpt.get().getItems());

            log.debug("Found {} permanent products in MongoDB for user {}", mongoCartOpt.get().getItems().size(), userId);
        }


        return ShoppingCart.builder()
                .userId(userId)
                .items(allItems)
                .build();
    }

    // =========================================================
    // ADD ITEM
    // =========================================================

    public ShoppingCart addItemToCart(
            String userId,
            AddToCartRequest request) {


        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }

        if (request == null) {
            throw new IllegalArgumentException("AddToCartRequest cannot be null");
        }

        if (request.getProductId() == null || request.getProductId().isBlank()) {

            throw new IllegalArgumentException("productId cannot be null or empty");
        }

        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException( "quantity must be greater than zero");
        }

        ShoppingCart redisCart = cartRepository.findById(userId)
                        .orElseGet(() ->
                                ShoppingCart.builder()
                                        .userId(userId)
                                        .items(new ArrayList<>())
                                        .build()
                        );

        if (redisCart.getItems() == null) {
            redisCart.setItems(new ArrayList<>());
        }

        CartItem redisItem = redisCart.getItems()
                        .stream()
                        .filter(item ->
                                request.getProductId()
                                        .equals(item.getProductId()))
                        .findFirst()
                        .orElse(null);

        if (redisItem != null) {


            int newQuantity = redisItem.getQuantity() + request.getQuantity();

            redisItem.setQuantity(newQuantity);


            cartRepository.save(redisCart);

            redisTemplate.opsForSet().add(ACTIVE_CARTS_KEY, userId);

            log.info(
                    "Redis product quantity increased. user={}, product={}, quantity={}",
                    userId,
                    request.getProductId(),
                    newQuantity
            );

            return getCart(userId);
        }

        
        Optional<ShoppingCartDocument> mongoCartOpt = cartMongoRepository.findById(userId);

        if (mongoCartOpt.isPresent() && mongoCartOpt.get().getItems() != null) {

            ShoppingCartDocument mongoCart = mongoCartOpt.get();

            CartItem mongoItem = mongoCart.getItems()
                            .stream()
                            .filter(item ->
                                    request.getProductId()
                                            .equals(item.getProductId()))
                            .findFirst()
                            .orElse(null);

            if (mongoItem != null) {


                int newQuantity = mongoItem.getQuantity() + request.getQuantity();

                mongoItem.setQuantity(newQuantity);

                updateMongoTotal(mongoCart);

                cartMongoRepository.save(mongoCart);

                log.info(
                        "Mongo product quantity increased. user={}, product={}, quantity={}",
                        userId,
                        request.getProductId(),
                        newQuantity
                );

                return getCart(userId);
            }
        }

        // Nouveau produit

        CartItem newItem =
                CartItem.builder()
                        .productId(request.getProductId())
                        .sellerId(request.getSellerId())
                        .productName(request.getProductName())
                        .imageUrl(request.getImageUrl())
                        .price(request.getPrice())
                        .quantity(request.getQuantity())
                        .addedAt(
                                Instant.now()
                                        .getEpochSecond()
                        )
                        .build();

        redisCart.getItems()
                .add(newItem);


        cartRepository.save(redisCart);

        redisTemplate.opsForSet()
                .add(
                        ACTIVE_CARTS_KEY,
                        userId
                );

        log.info(
                "New product added to Redis. user={}, product={}",
                userId,
                request.getProductId()
        );

        return getCart(userId);
    }

    // REMOVE ITEM

    public ShoppingCart removeItemFromCart(
            String userId,
            String productId) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "userId cannot be null or empty"
            );
        }

        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException(
                    "productId cannot be null or empty"
            );
        }

        // Supprimer de Redis

        Optional<ShoppingCart> redisCartOpt =
                cartRepository.findById(userId);

        if (redisCartOpt.isPresent()) {

            ShoppingCart redisCart =
                    redisCartOpt.get();

            if (redisCart.getItems() != null) {

                boolean removed =
                        redisCart.getItems()
                                .removeIf(item ->
                                        productId.equals(
                                                item.getProductId()
                                        ));

                if (removed) {

                    if (redisCart.getItems().isEmpty()) {

                        cartRepository.deleteById(userId);

                        redisTemplate.opsForSet()
                                .remove(
                                        ACTIVE_CARTS_KEY,
                                        userId
                                );

                    } else {

                        cartRepository.save(redisCart);
                    }

                    log.info(
                            "Product removed from Redis. user={}, product={}",
                            userId,
                            productId
                    );
                }
            }
        }

        // Supprimer de MongoDB

        Optional<ShoppingCartDocument> mongoCartOpt =
                cartMongoRepository.findById(userId);

        if (mongoCartOpt.isPresent()) {

            ShoppingCartDocument mongoCart =
                    mongoCartOpt.get();

            if (mongoCart.getItems() != null) {

                boolean removed =
                        mongoCart.getItems()
                                .removeIf(item ->
                                        productId.equals(
                                                item.getProductId()
                                        ));

                if (removed) {

                    if (mongoCart.getItems().isEmpty()) {

                        cartMongoRepository.deleteById(
                                userId
                        );

                    } else {

                        updateMongoTotal(mongoCart);

                        cartMongoRepository.save(
                                mongoCart
                        );
                    }

                    log.info(
                            "Product removed from MongoDB. user={}, product={}",
                            userId,
                            productId
                    );
                }
            }
        }

        return getCart(userId);
    }

    
    public void clearCart(String userId) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "userId cannot be null or empty"
            );
        }

        cartRepository.deleteById(userId);

        redisTemplate.opsForSet()
                .remove(
                        ACTIVE_CARTS_KEY,
                        userId
                );

        cartMongoRepository.deleteById(userId);

        log.info(
                "Cart completely cleared for user {}",
                userId
        );
    }

    // REDIS -> MONGO MIGRATION
    // @Scheduled(cron = "0 0 * * * *")
    @Scheduled(cron = "0 0 1 * * *")
    public void backupOldCartItemsToMongoDB() {

        log.info("Starting Redis -> MongoDB cart migration...");

        // Récupérer les utilisateurs actifs

        Set<String> activeUsers = redisTemplate.opsForSet().members(ACTIVE_CARTS_KEY);

        if (activeUsers == null || activeUsers.isEmpty()) {

            log.info("No active carts found in Redis");

            return;
        }

        long now = Instant.now().getEpochSecond();


        for (String userId : activeUsers) {

            try {

                migrateUserCart(
                        userId,
                        now
                );

            } catch (Exception e) {

                log.error(
                        "Failed to migrate cart for user {}",
                        userId,
                        e
                );
            }
        }

        log.info(
                "Redis -> MongoDB cart migration finished."
        );
    }


    private void migrateUserCart(
            String userId,
            long now) {

        // Récupérer le panier Redis

        Optional<ShoppingCart> cartOpt =
                cartRepository.findById(userId);

        if (cartOpt.isEmpty()) {

            redisTemplate.opsForSet()
                    .remove(
                            ACTIVE_CARTS_KEY,
                            userId
                    );

            log.debug(
                    "Removed stale active cart reference for user {}",
                    userId
            );

            return;
        }

        ShoppingCart redisCart =
                cartOpt.get();

        if (redisCart.getItems() == null
                || redisCart.getItems().isEmpty()) {

            cartRepository.deleteById(userId);

            redisTemplate.opsForSet()
                    .remove(
                            ACTIVE_CARTS_KEY,
                            userId
                    );

            return;
        }

        
        List<CartItem> itemsToMongo =
                new ArrayList<>();

        List<CartItem> itemsToKeepInRedis =
                new ArrayList<>();

        for (CartItem item :
                redisCart.getItems()) {


            if (item.getAddedAt() == null) {

                log.warn(
                        "Product {} has no addedAt. Keeping it in Redis.",
                        item.getProductId()
                );

                itemsToKeepInRedis.add(item);

                continue;
            }


            long itemAge =
                    now - item.getAddedAt();

            // Protection contre une date future
            if (itemAge < 0) {

                log.warn(
                        "Product {} has future addedAt: {}",
                        item.getProductId(),
                        item.getAddedAt()
                );

                itemsToKeepInRedis.add(item);

                continue;
            }


            if (itemAge >= SEVEN_DAYS_SECONDS) {

                itemsToMongo.add(item);

            } else {

                itemsToKeepInRedis.add(item);
            }
        }


        if (itemsToMongo.isEmpty()) {
            return;
        }

        // Récupérer/créer le panier Mongo

        ShoppingCartDocument mongoCart =
                cartMongoRepository
                        .findById(userId)
                        .orElseGet(() ->
                                ShoppingCartDocument.builder()
                                        .userId(userId)
                                        .items(new ArrayList<>())
                                        .build()
                        );

        if (mongoCart.getItems() == null) {

            mongoCart.setItems(
                    new ArrayList<>()
            );
        }

        // Déplacer les produits

        for (CartItem item :
                itemsToMongo) {

            Optional<CartItem> existingMongoItem =
                    mongoCart.getItems()
                            .stream()
                            .filter(existing ->
                                    item.getProductId()
                                            .equals(
                                                    existing.getProductId()
                                            ))
                            .findFirst();

            if (existingMongoItem.isPresent()) {

                CartItem mongoItem =
                        existingMongoItem.get();

                mongoItem.setQuantity(
                        item.getQuantity()
                );

                
                mongoItem.setSellerId(
                        item.getSellerId()
                );

                mongoItem.setProductName(
                        item.getProductName()
                );

                mongoItem.setImageUrl(
                        item.getImageUrl()
                );

                mongoItem.setPrice(
                        item.getPrice()
                );

            } else {

                // Nouveau produit permanent
                mongoCart.getItems()
                        .add(item);
            }
        }


        updateMongoTotal(mongoCart);


        cartMongoRepository.save(
                mongoCart
        );


        if (itemsToKeepInRedis.isEmpty()) {

            // Aucun produit jeune ne reste
            cartRepository.deleteById(userId);

            redisTemplate.opsForSet()
                    .remove(
                            ACTIVE_CARTS_KEY,
                            userId
                    );

        } else {

            redisCart.setItems(
                    itemsToKeepInRedis
            );

            cartRepository.save(
                    redisCart
            );

            redisTemplate.opsForSet()
                    .add(
                            ACTIVE_CARTS_KEY,
                            userId
                    );
        }

        log.info(
                "Moved {} products from Redis to MongoDB for user {}",
                itemsToMongo.size(),
                userId
        );
    }


    /*** Recalcule le montant total du panier MongoDB.*/
    private void updateMongoTotal(
            ShoppingCartDocument mongoCart) {

        if (mongoCart.getItems() == null
                || mongoCart.getItems().isEmpty()) {

            mongoCart.setTotalAmount(
                    BigDecimal.ZERO
            );

            return;
        }

        BigDecimal total =
                mongoCart.getItems()
                        .stream()
                        .filter(item ->
                                item.getPrice() != null)
                        .map(item ->
                                item.getPrice()
                                        .multiply(
                                                BigDecimal.valueOf(
                                                        item.getQuantity()
                                                )
                                        )
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        mongoCart.setTotalAmount(total);
    }
}
