package buy01.order_service.dto;

import buy01.order_service.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}