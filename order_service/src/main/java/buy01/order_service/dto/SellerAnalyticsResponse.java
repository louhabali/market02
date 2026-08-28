package buy01.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellerAnalyticsResponse {
    private BigDecimal totalRevenue;
    private long totalUnitsSold;
    private long totalOrders;
    private List<TopProduct> topProducts;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopProduct {
        private String productId;
        private String name;
        private long unitsSold;
        private BigDecimal revenue;
    }
}
