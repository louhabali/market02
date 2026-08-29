package buy01.product_service.controller;

import buy01.product_service.model.Product;
import buy01.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable String id) {
        return productService.getProduct(id);
    }
    @GetMapping("/search")
    public Page<Product> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Sort.Direction sortDir = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));

        return productService.searchProducts(keyword, category, minPrice, maxPrice, pageable);
    }

    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Product createProduct(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Role") String userRole,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam Double price,
            @RequestParam Integer quantity,
            @RequestParam String category,
            @RequestParam(required = false) MultipartFile[] images
    ) {
        return productService.createProduct(
                name,
                description,
                price,
                quantity,
                category,
                images,
                userId,
                userRole
        );
    }

   @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Product> updateProduct(
        @PathVariable String id,
        @RequestParam("name") String name,
        @RequestParam("description") String description,
        @RequestParam("price") Double price,
        @RequestParam("quantity") Integer quantity,
        @RequestParam("category") String category,
        @RequestParam(value = "existingImageUrls", required = false) List<String> existingImageUrls,
        @RequestPart(value = "images", required = false) MultipartFile[] images,
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-Role") String userRole) {

        Product updated = productService.updateProduct(
            id, name, description, price, quantity, category, existingImageUrls, images, userId, userRole);
    return ResponseEntity.ok(updated);
}

    @DeleteMapping("/{id}")
    public void deleteProduct(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Role") String userRole
    ) {
        productService.deleteProduct(id, userId, userRole);
    }
}