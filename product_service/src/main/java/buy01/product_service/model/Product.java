package buy01.product_service.model;

import lombok.*;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String name;

    private String description;

    private Double price;

    private Integer quantity;

    private String userId;

    private String category;
    
    private List<String> imageUrls;
}