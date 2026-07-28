package buy01.product_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import buy01.product_service.model.Product;

public final class ProductAssertions {

        private ProductAssertions() {
        }

        public static void assertProductEquals(Product expected,
                        Product actual) {

                assertThat(actual).isNotNull();

                assertThat(actual.getId())
                                .isEqualTo(expected.getId());

                assertThat(actual.getName())
                                .isEqualTo(expected.getName());

                assertThat(actual.getDescription())
                                .isEqualTo(expected.getDescription());

                assertThat(actual.getPrice())
                                .isEqualTo(expected.getPrice());

                assertThat(actual.getQuantity())
                                .isEqualTo(expected.getQuantity());

                assertThat(actual.getUserId())
                                .isEqualTo(expected.getUserId());

                assertThat(actual.getImageUrls())
                                .containsExactlyElementsOf(expected.getImageUrls());
        }

        public static void assertProductListEquals(
                        List<Product> expected,
                        List<Product> actual) {

                assertThat(actual)
                                .hasSize(expected.size());

                for (int i = 0; i < expected.size(); i++) {
                        assertProductEquals(expected.get(i), actual.get(i));
                }
        }
}
