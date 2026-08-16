package buy01.order_service.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String id) {
        super("Order not found with id: " + id);
    }
}