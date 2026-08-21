package vn.dynamicshop.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import vn.dynamicshop.catalog.Product;

/**
 * Bất biến #3 — tiền là {@code long}, đơn vị đồng. Không {@code double}/{@code float}/
 * {@code BigDecimal} trong domain. Stage 0 chưa duyệt thêm ArchUnit (không có trong danh
 * sách dependency đã duyệt) nên kiểm tra bằng reflection thủ công trên các entity tiền
 * chạm tới — vẫn bắt được lỗi, chỉ là không tự động quét toàn bộ classpath như ArchUnit.
 */
class MoneyTypeTest {

    private static final List<Class<?>> FORBIDDEN_MONEY_TYPES = List.of(double.class, Double.class,
            float.class, Float.class, BigDecimal.class);

    @Test
    void order_khong_dung_double_hay_bigdecimal_cho_tien() {
        assertNoForbiddenMoneyFields(Order.class);
        assertNoForbiddenMoneyFields(OrderItem.class);
        assertNoForbiddenMoneyFields(Product.class);
    }

    private void assertNoForbiddenMoneyFields(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            assertThat(FORBIDDEN_MONEY_TYPES).as("field %s.%s dùng kiểu %s bị cấm cho tiền",
                            type.getSimpleName(), field.getName(), field.getType())
                    .doesNotContain(field.getType());
        }
    }
}
