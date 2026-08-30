package buy01.cart_service.service;

import buy01.cart_service.dto.AddToCartRequest;
import buy01.cart_service.dto.UpdateQuantityRequest;
import buy01.cart_service.model.CartItem;
import buy01.cart_service.model.ShoppingCart;
import buy01.cart_service.model.ShoppingCartDocument;
import buy01.cart_service.repo.CartMongoRepository;
import buy01.cart_service.repo.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final String USER_ID = "user-1";
    private static final String ACTIVE_CARTS_KEY = "active_carts";

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartMongoRepository cartMongoRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ProductStockClient productStockClient;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        cartService = new CartService(cartRepository, cartMongoRepository, redisTemplate, productStockClient);
    }

    @Test
    void shouldMergeRedisAndMongoItems() {
        ShoppingCart redisCart = ShoppingCart.builder()
                .userId(USER_ID)
                .items(new ArrayList<>(List.of(
                        item("p-1", 2, new BigDecimal("10.00")),
                        item("p-2", 1, new BigDecimal("15.00"))
                )))
                .build();

        ShoppingCartDocument mongoCart = ShoppingCartDocument.builder()
                .userId(USER_ID)
                .items(new ArrayList<>(List.of(
                        item("p-3", 3, new BigDecimal("20.00"))
                )))
                .build();

        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(redisCart));
        when(cartMongoRepository.findById(USER_ID)).thenReturn(Optional.of(mongoCart));

        ShoppingCart result = cartService.getCart(USER_ID);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getItems()).hasSize(3);
        assertThat(result.getItems())
                .extracting(CartItem::getProductId)
                .containsExactlyInAnyOrder("p-1", "p-2", "p-3");
    }

    @Test
    void shouldIncreaseQuantityWhenProductAlreadyExistsInRedis() {
        ShoppingCart redisCart = ShoppingCart.builder()
                .userId(USER_ID)
                .items(new ArrayList<>(List.of(
                        item("p-1", 2, new BigDecimal("10.00"))
                )))
                .build();

        AddToCartRequest request = AddToCartRequest.builder()
                .productId("p-1")
                .sellerId("seller-1")
                .productName("Product p-1")
                .imageUrl("https://example.com/p-1.png")
                .price(new BigDecimal("10.00"))
                .quantity(3)
                .build();

        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(redisCart));
        when(productStockClient.getAvailableQuantity("p-1")).thenReturn(10);
        when(cartRepository.save(redisCart)).thenReturn(redisCart);
        when(cartMongoRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ShoppingCart result = cartService.addItemToCart(USER_ID, request);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
        verify(cartRepository).save(redisCart);
        verify(setOperations).add(ACTIVE_CARTS_KEY, USER_ID);
    }

    @Test
    void shouldRejectInvalidAddItemRequest() {
        AddToCartRequest invalidRequest = AddToCartRequest.builder()
                .productId("p-1")
                .sellerId("seller-1")
                .productName("Product p-1")
                .imageUrl("https://example.com/p-1.png")
                .price(new BigDecimal("10.00"))
                .quantity(0)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> cartService.addItemToCart(USER_ID, invalidRequest));
        assertThrows(IllegalArgumentException.class,
                () -> cartService.addItemToCart("", invalidRequest));
    }

    @Test
    void shouldRejectAddingMoreThanAvailableStock() {
        ShoppingCart redisCart = ShoppingCart.builder()
                .userId(USER_ID)
                .items(new ArrayList<>())
                .build();

        AddToCartRequest request = AddToCartRequest.builder()
                .productId("p-1")
                .sellerId("seller-1")
                .productName("Product p-1")
                .imageUrl("https://example.com/p-1.png")
                .price(new BigDecimal("10.00"))
                .quantity(5)
                .build();

        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(redisCart));
        when(productStockClient.getAvailableQuantity("p-1")).thenReturn(3);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cartService.addItemToCart(USER_ID, request));

        assertThat(ex.getMessage()).contains("Only 3 item");
    }

    @Test
    void shouldConvertStockLookupFailuresToUserFriendlyBadRequest() {
        ShoppingCart redisCart = ShoppingCart.builder()
                .userId(USER_ID)
                .items(new ArrayList<>())
                .build();

        AddToCartRequest request = AddToCartRequest.builder()
                .productId("p-1")
                .sellerId("seller-1")
                .productName("Product p-1")
                .imageUrl("https://example.com/p-1.png")
                .price(new BigDecimal("10.00"))
                .quantity(1)
                .build();

        when(productStockClient.getAvailableQuantity("p-1")).thenThrow(new RuntimeException("product service unavailable"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cartService.addItemToCart(USER_ID, request));

        assertThat(ex.getMessage()).contains("Unable to verify stock");
    }

    @Test
    void shouldRemoveItemAndDeleteRedisCartWhenEmpty() {
        ShoppingCart redisCart = ShoppingCart.builder()
                .userId(USER_ID)
                .items(new ArrayList<>(List.of(
                        item("p-1", 1, new BigDecimal("10.00"))
                )))
                .build();

        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(redisCart));
        when(cartMongoRepository.findById(USER_ID)).thenReturn(Optional.empty());

        cartService.removeItemFromCart(USER_ID, "p-1");

        verify(cartRepository).deleteById(USER_ID);
        verify(setOperations).remove(ACTIVE_CARTS_KEY, USER_ID);
    }

    @Test
    void shouldThrowWhenUpdatingMissingProduct() {
        ShoppingCart redisCart = ShoppingCart.builder()
                .userId(USER_ID)
                .items(new ArrayList<>(List.of(
                        item("p-1", 1, new BigDecimal("10.00"))
                )))
                .build();

        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(redisCart));

        UpdateQuantityRequest request = new UpdateQuantityRequest("missing-product", 3);

        assertThrows(NoSuchElementException.class,
                () -> cartService.updateItemQuantity(USER_ID, request));
    }

    @Test
    void shouldClearCartFromBothStores() {
        cartService.clearCart(USER_ID);

        verify(cartRepository).deleteById(USER_ID);
        verify(cartMongoRepository).deleteById(USER_ID);
        verify(setOperations).remove(ACTIVE_CARTS_KEY, USER_ID);
    }

    private CartItem item(String productId, int quantity, BigDecimal price) {
        return CartItem.builder()
                .productId(productId)
                .sellerId("seller-1")
                .productName("Product " + productId)
                .imageUrl("https://example.com/" + productId + ".png")
                .price(price)
                .quantity(quantity)
                .addedAt(1_700_000_000L)
                .build();
    }
}
