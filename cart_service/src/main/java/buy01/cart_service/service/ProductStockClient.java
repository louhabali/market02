package buy01.cart_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductStockClient {

    private final RestTemplate restTemplate;
    private final String productServiceUrl;

    public ProductStockClient(RestTemplate restTemplate,
                             @Value("${product.service.url:http://product-service:8082}") String productServiceUrl) {
        this.restTemplate = restTemplate;
        this.productServiceUrl = productServiceUrl;
    }

    public int getAvailableQuantity(String productId) {
        String url = productServiceUrl + "/api/products/" + productId + "/available-stock";
        Integer quantity = restTemplate.getForObject(url, Integer.class);
        return quantity == null ? 0 : quantity;
    }
}
