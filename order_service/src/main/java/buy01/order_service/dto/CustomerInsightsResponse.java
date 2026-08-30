package buy01.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerInsightsResponse {
    private BigDecimal totalSpent;
    private long totalOrders;
    private String topCategory;
}
