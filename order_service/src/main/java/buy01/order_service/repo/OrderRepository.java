package buy01.order_service.repo;

import buy01.order_service.model.Order;
import buy01.order_service.model.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    // Find all orders placed by a specific customer
    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    // Find all orders containing products from a specific seller
    @Query("{ 'items.sellerId': ?0 }")
    List<Order> findBySellerId(String sellerId);

    // Find orders by customer and status
    List<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status);
}