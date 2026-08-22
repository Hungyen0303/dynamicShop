package vn.dynamicshop.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Shape khớp đúng {@code docs/30-backend.md} mục "Endpoint polling cho merchant":
 * {@code { orders: [...], server_time: "…", has_more: false }}.
 *
 * ⚠️ {@code server_time}/{@code has_more} là snake_case trong khi phần còn lại của API dùng
 * camelCase. Giữ nguyên theo văn bản đã chốt thay vì tự ý chuẩn hoá — xem {@code aware.md},
 * chủ dự án quyết có đổi hay không.
 *
 * {@code serverTime} để merchant_app lấy mốc {@code since} cho lần poll sau từ ĐỒNG HỒ CỦA
 * SERVER, không phải đồng hồ máy mình. Đồng hồ điện thoại lệch vài phút là chuyện thường,
 * và lệch về phía tương lai sẽ khiến app bỏ qua vĩnh viễn những đơn nằm trong khoảng lệch —
 * đúng loại lỗi mất đơn im lặng mà docs/11-merchant-app.md coi là nghiêm trọng nhất.
 */
public record OrderSyncResponseDto(
        List<OrderSummaryDto> orders,
        @JsonProperty("server_time") Instant serverTime,
        @JsonProperty("has_more") boolean hasMore) {
}
