package buy01.cart_service.repo;

import buy01.cart_service.model.ShoppingCartDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartMongoRepository extends MongoRepository<ShoppingCartDocument, String> {
    void deleteByExpiresAtBefore(Long timestamp);
}