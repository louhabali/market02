package buy01.order_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShippingAddress {
    private String fullName;
    private String phone;
    private String streetAddress;
    private String city;
    private String postalCode;
}