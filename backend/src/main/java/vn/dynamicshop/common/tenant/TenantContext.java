package vn.dynamicshop.common.tenant;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * Tenant hiện tại của request đang xử lý trên thread này.
 *
 * Nguồn duy nhất được phép ghi vào đây: {@code PublicTenantSlugFilter} (slug trong path,
 * public plane) hoặc {@code JwtAuthenticationFilter} (JWT claim, authenticated plane).
 * KHÔNG BAO GIỜ set từ @RequestParam hay request body — bất biến #1.
 *
 * ThreadLocal vì Tomcat tái sử dụng worker thread giữa các request — filter phải luôn
 * {@link #clear()} trong khối finally, nếu không sẽ rò rỉ tenant qua thread pool (khác
 * với lỗ rò rỉ qua connection pool, nhưng cùng bản chất: quên dọn state gắn với thread).
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();
    private static final String MDC_KEY = "tenantId";

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
        MDC.put(MDC_KEY, tenantId == null ? null : tenantId.toString());
    }

    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /** Bắt buộc có tenant — dùng trong code chỉ chạy trong tenant plane, ném lỗi nếu thiếu. */
    public static UUID require() {
        return current().orElseThrow(() -> new IllegalStateException(
                "TenantContext trống — endpoint này cần tenant từ slug hoặc JWT claim"));
    }

    public static void clear() {
        CURRENT.remove();
        MDC.remove(MDC_KEY);
    }
}
