package buy01.cart_service.repository;

import buy01.cart_service.model.ShoppingCart;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends CrudRepository<ShoppingCart, String> {
}