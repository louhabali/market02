package buy01.product_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

// import buy01.product_service.controller.ProductController;
import buy01.product_service.model.Product;
import buy01.product_service.service.ProductService;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void shouldReturnAllProductsFromController() throws Exception {
        Product product = Product.builder()
                .id("1")
                .name("Gaming Laptop")
                .description("RTX 4090 Gaming Laptop")
                .price(1999.99)
                .quantity(5)
                .userId("seller-1")
                .imageUrls(List.of("img1.jpg"))
                .build();

        when(productService.getAllProducts()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Gaming Laptop"))
                .andExpect(jsonPath("$[0].description").value("RTX 4090 Gaming Laptop"));
    }

    @Test
    void shouldCreateProductViaController() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "images",
                "product.jpg",
                "image/jpeg",
                "image".getBytes());

        Product createdProduct = Product.builder()
                .id("1")
                .name("Gaming Laptop")
                .description("RTX 4090 Gaming Laptop")
                .price(1999.99)
                .quantity(5)
                .userId("seller-1")
                .imageUrls(List.of("img1.jpg"))
                .build();

        when(productService.createProduct(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(createdProduct);

        mockMvc.perform(multipart("/api/products")
                .file(new org.springframework.mock.web.MockMultipartFile("images", "product.jpg", "image/jpeg",
                        image.getBytes()))
                .header("X-User-Id", "seller-1")
                .header("X-Role", "SELLER")
                .param("name", "Gaming Laptop")
                .param("description", "RTX 4090 Gaming Laptop")
                .param("price", "1999.99")
                .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gaming Laptop"));
    }
}
