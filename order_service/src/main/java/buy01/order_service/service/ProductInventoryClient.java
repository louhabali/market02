package buy01.order_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductInventoryClient {

    private final RestTemplate restTemplate;
    private final String productServiceUrl;

    public ProductInventoryClient(RestTemplate restTemplate,
                                 @Value("${product.service.url:http://product-service:8082}") String productServiceUrl) {
        this.restTemplate = restTemplate;
        this.productServiceUrl = productServiceUrl;
    }

    public int getAvailableQuantity(String productId) {
        String url = productServiceUrl + "/api/products/" + productId + "/available-stock";
        try {
            Integer quantity = restTemplate.getForObject(url, Integer.class);
            return quantity == null ? 0 : quantity;
        } catch (HttpClientErrorException.NotFound ex) {
            return 0;
        }
    }

    public void decrementStock(String productId, int quantity) {
        String url = productServiceUrl + "/api/products/" + productId + "/decrement-stock";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("quantity", String.valueOf(quantity));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        try {
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new IllegalStateException(ex.getResponseBodyAsString());
        } catch (HttpClientErrorException.NotFound ex) {
            throw new IllegalStateException("Product not found while updating inventory.");
        }
    }
}
