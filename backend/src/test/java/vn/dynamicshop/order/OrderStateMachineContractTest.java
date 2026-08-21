package vn.dynamicshop.order;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Drift guard Stage 0 (không có generator, xem contracts/README.md): đọc THẲNG
 * {@code contracts/order-states.json} và so sánh với enum Java. Sửa contract mà quên sửa
 * {@link OrderStatus}/{@link PaymentStatus} sẽ đỏ ở đây.
 */
class OrderStateMachineContractTest {

    private static final ObjectMapper JACKSON = new ObjectMapper();

    @Test
    void order_status_transitions_khop_contract() throws IOException {
        JsonNode root = readContract();
        JsonNode orderStatusNode = root.get("order_status").get("transitions");

        // So khớp CẢ HAI CHIỀU: contract cho phép mà enum từ chối, hoặc enum cho phép mà
        // contract không có — cả hai đều là drift, đều phải đỏ.
        for (OrderStatus from : OrderStatus.values()) {
            JsonNode allowed = orderStatusNode.get(from.name());
            for (OrderStatus to : OrderStatus.values()) {
                boolean allowedByContract = contains(allowed, to.name());
                assertThat(from.canTransitionTo(to))
                        .as("enum Java cho phép %s -> %s nhưng contract không có", from, to)
                        .isEqualTo(allowedByContract);
            }
        }
    }

    @Test
    void payment_status_transitions_khop_contract() throws IOException {
        JsonNode root = readContract();
        JsonNode paymentStatusNode = root.get("payment_status").get("transitions");

        for (PaymentStatus from : PaymentStatus.values()) {
            JsonNode allowed = paymentStatusNode.get(from.name());
            for (PaymentStatus to : PaymentStatus.values()) {
                boolean allowedByContract = contains(allowed, to.name());
                assertThat(from.canTransitionTo(to))
                        .as("payment_status %s -> %s lệch giữa contract và enum", from, to)
                        .isEqualTo(allowedByContract);
            }
        }
    }

    private boolean contains(JsonNode arrayNode, String value) {
        if (arrayNode == null) {
            return false;
        }
        for (JsonNode n : arrayNode) {
            if (n.asText().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private JsonNode readContract() throws IOException {
        // backend/ là working dir khi Gradle chạy test — contracts/ nằm ở root monorepo.
        Path path = Path.of("..", "contracts", "order-states.json");
        assertThat(Files.exists(path)).as("Không tìm thấy %s", path.toAbsolutePath()).isTrue();
        return JACKSON.readTree(path.toFile());
    }
}
