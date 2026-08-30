package buy01.product_service.service;

import buy01.product_service.client.MediaClient;
import buy01.product_service.exceptions.ForbiddenException;
import buy01.product_service.model.Product;
import buy01.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Pageable;

import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final MediaClient mediaClient;
    private final MongoTemplate mongoTemplate;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif", "image/avif", "image/x-avif");
    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB
    private static final int MAX_IMAGES_COUNT = 5;
        private static final List<String> ALLOWED_CATEGORIES = Arrays.asList(
            "Streetwear", "Outerwear", "Accessories");

    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    public Product getProduct(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public int getAvailableQuantity(String id) {
        Product product = getProduct(id);
        return product.getQuantity() == null ? 0 : product.getQuantity();
    }

    public Product decrementStock(String productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order quantity must be greater than zero.");
        }

        Product product = getProduct(productId);
        int available = product.getQuantity() == null ? 0 : product.getQuantity();

        if (quantity > available) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only " + available + " item(s) available in stock for this product.");
        }

        product.setQuantity(available - quantity);
        return repository.save(product);
    }

    
    // Search and filter products with pagination
    public Page<Product> searchProducts(
            String keyword, 
            String category, 
            Double minPrice, 
            Double maxPrice, 
            Pageable pageable) {

        Query query = new Query();

        //  Filter by keyword in name or description
        if (keyword != null && !keyword.trim().isEmpty()) {
            String cleanKw = keyword.trim();
            Criteria nameMatch = Criteria.where("name").regex(cleanKw, "i");
            Criteria descMatch = Criteria.where("description").regex(cleanKw, "i");
            query.addCriteria(new Criteria().orOperator(nameMatch, descMatch));
        }

        //  Filter by category
        if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            query.addCriteria(Criteria.where("category").is(category.trim()));
        }

        //  Filter by price range
        if (minPrice != null && maxPrice != null) {
            query.addCriteria(Criteria.where("price").gte(minPrice).lte(maxPrice));
        } else if (minPrice != null) {
            query.addCriteria(Criteria.where("price").gte(minPrice));
        } else if (maxPrice != null) {
            query.addCriteria(Criteria.where("price").lte(maxPrice));
        }

        // Get total elements for pagination count
        long total = mongoTemplate.count(query, Product.class);

        // Apply page size and page number limits
        query.with(pageable);

        List<Product> products = mongoTemplate.find(query, Product.class);

        return new PageImpl<>(products, pageable, total);
    }


    public Product createProduct(
            String name,
            String description,
            Double price,
            Integer quantity,
            String category,
            MultipartFile[] images,
            String userId,
            String userRole) {

        validateUserData(userId);

        if (!"SELLER".equalsIgnoreCase(userRole)) {
            throw new ForbiddenException("You do not have permission to perform this action.");
        }

        String validatedCategory = validateProductDetails(name, description, price, quantity, category);

        List<String> imageUrls = new ArrayList<>();
        if (hasValidImages(images)) {
            validateImages(images);
            imageUrls = mediaClient.uploadImages(images);
        }
        Product product = Product.builder()
                .name(name.trim())
                .description(description.trim())
                .price(price)
                .quantity(quantity)
                .userId(userId)
                .category(validatedCategory)
                .imageUrls(imageUrls)
                .build();

        return repository.save(product);
    }

    public Product updateProduct(
            String id,
            String name,
            String description,
            Double price,
            Integer quantity,
            String category,
            List<String> existingImageUrls,
            MultipartFile[] newImages,
            String userId,
            String userRole) {

        Product product = getProduct(id);
        verifyOwnership(product, userId);

        String validatedCategory = validateProductDetails(name, description, price, quantity, category);

        product.setName(name.trim());
        product.setDescription(description.trim());
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setCategory(validatedCategory);

        // 1. Start with remaining existing URLs passed from frontend
        List<String> finalImageUrls = new ArrayList<>();
        if (existingImageUrls != null) {
            finalImageUrls.addAll(existingImageUrls);
        }

        // 2. Upload and append any newly added images
        if (hasValidImages(newImages)) {
            validateImages(newImages);
            List<String> newlyUploadedUrls = mediaClient.uploadImages(newImages);
            finalImageUrls.addAll(newlyUploadedUrls);
        }

        // 3. Enforce maximum total allowed images limit across both existing and new
        if (finalImageUrls.size() > MAX_IMAGES_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Total images cannot exceed " + MAX_IMAGES_COUNT + " per product.");
        }

        product.setImageUrls(finalImageUrls);

        return repository.save(product);
    }

    public void deleteProduct(String id, String userId, String userRole) {
        Product product = getProduct(id);
        verifyOwnership(product, userId);
        repository.delete(product);
    }

    public void deleteProductsByUserId(String userId) {
       
        if (userId != null && !userId.isBlank()) {
            repository.deleteByUserId(userId);
        }
    }

    private void verifyOwnership(Product product, String userId) {
        validateUserData(userId);

        boolean isOwner = product.getUserId() != null && product.getUserId().equals(userId);

        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Unauthorized action");
        }
    }

    private void validateUserData(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User context missing or unauthenticated");
        }
    }

        private String validateProductDetails(
            String name, String description, Double price, Integer quantity, String category) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is required.");
        }
        String trimmedName = name.trim();
        if (trimmedName.length() < 3 || trimmedName.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product name must be between 3 and 100 characters.");
        }

        if (description == null || description.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required.");
        }
        String trimmedDesc = description.trim();
        if (trimmedDesc.length() < 10 || trimmedDesc.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Description must be between 10 and 1000 characters.");
        }

        if (price == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price is required.");
        }
        if (price < 0.01) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be at least 0.01 DH.");
        }
        if (price > 9999999.99) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price cannot exceed 9,999,999.99 DH.");
        }
        if (BigDecimal.valueOf(price).scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price cannot have more than 2 decimal places.");
        }

        if (quantity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity is required.");
        }
        if (quantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity cannot be negative.");
        }
        if (quantity > 999999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity cannot exceed 999,999 units.");
        }

        if (category == null || category.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is required.");
        }
        String trimmedCategory = category.trim();
        if (!ALLOWED_CATEGORIES.contains(trimmedCategory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Category must be one of: " + String.join(", ", ALLOWED_CATEGORIES) + ".");
        }
        return trimmedCategory;
    }

    private void validateImages(MultipartFile[] images) {
        if (!hasValidImages(images)) {
            return;
        }

        if (images.length > MAX_IMAGES_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Maximum " + MAX_IMAGES_COUNT + " images allowed per product.");
        }

        for (MultipartFile file : images) {
            if (file.isEmpty()) {
                continue;
            }

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid file type for '" + file.getOriginalFilename()
                                + "'. Only JPG, PNG, WEBP, GIF, and AVIF are allowed.");
            }

            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "File '" + file.getOriginalFilename() + "' exceeds the 2MB size limit.");
            }
        }
    }

    private boolean hasValidImages(MultipartFile[] images) {
        return images != null && images.length > 0 && !images[0].isEmpty();
    }
}