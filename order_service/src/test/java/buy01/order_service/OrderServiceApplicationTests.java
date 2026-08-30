package buy01.order_service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OrderServiceApplicationTests {

    @Test
    void applicationClassCanBeLoaded() {
        assertDoesNotThrow(OrderServiceApplication::new);
    }

}
