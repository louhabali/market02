package buy01.product_service.lkwa;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import buy01.product_service.client.MediaClient;
import buy01.product_service.model.Product;
import buy01.product_service.repository.ProductRepository;
import buy01.product_service.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private MediaClient mediaClient;

    @InjectMocks
    private ProductService service;

    private Product product;

    private MultipartFile image;

    @BeforeEach
    void setup() {

        image = new MockMultipartFile(
                "image",
                "product.jpg",
                "image/jpeg",
                "image".getBytes());

        product = Product.builder()
                .id("1")
                .name("Gaming Laptop")
                .description("RTX 4090 Gaming Laptop")
                .price(1999.99)
                .quantity(5)
                .userId("seller-1")
                .imageUrls(List.of("img1.jpg"))
                .build();
    }

    @Test
    void shouldGetProductById() {
    }

    @Test
    void shouldReturnAllProducts() {
    }

    // @Test
    // void shouldCreateProductWithoutImages() {
    // }

    @Test
    void shouldCreateProductWithImages() {
    }

    @Test
    void shouldUpdateProductWithoutUploadingNewImages() {
    }

    @Test
    void shouldUpdateProductWithNewImages() {
    }

    @Test
    void shouldDeleteProduct() {
    }

    @Test
    void shouldDeleteProductsByUserId() {
    }

    @Test
    void shouldThrowWhenProductDoesNotExist() {
    }

    @Test
    void shouldThrowWhenUserIsNotSeller() {
    }

    @Test
    void shouldThrowWhenUserDoesNotOwnProduct() {
    }

    @Test
    void shouldThrowWhenUserIdMissing() {
    }

    @Test
    void shouldThrowWhenNameIsBlank() {
    }

    @Test
    void shouldThrowWhenDescriptionTooShort() {
    }

    @Test
    void shouldThrowWhenPriceIsNegative() {
    }

    @Test
    void shouldThrowWhenPriceHasMoreThanTwoDecimals() {
    }

    @Test
    void shouldThrowWhenQuantityIsNegative() {
    }

    @Test
    void shouldThrowWhenTooManyImagesUploaded() {
    }

    @Test
    void shouldThrowWhenImageTypeIsInvalid() {
    }

    @Test
    void shouldThrowWhenImageTooLarge() {
    }

    @Test
    void shouldThrowWhenTotalImagesExceedLimit() {
    }

    @Test
    void shouldCreateProductWithoutImages() {

        Product expected = Product.builder()
                .id("1")
                .name("Gaming Laptop")
                .description("RTX 4090 Gaming Laptop")
                .price(1999.99)
                .quantity(5)
                .userId("seller-1")
                .imageUrls(List.of())
                .build();

        when(repository.save(any(Product.class)))
                .thenReturn(expected);

        Product result = service.createProduct(
                "Gaming Laptop",
                "RTX 4090 Gaming Laptop",
                1999.99,
                5,
                null,
                "seller-1",
                "SELLER");

        ProductAssertions.assertProductEquals(expected, result);

        verify(repository).save(any(Product.class));
        verifyNoInteractions(mediaClient);
    }
}