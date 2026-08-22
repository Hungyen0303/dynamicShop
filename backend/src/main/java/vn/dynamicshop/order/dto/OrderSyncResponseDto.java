package vn.dynamicshop.order.dto;

import java.time.Instant;
import java.util.List;

/**
 * Phản hồi của {@code GET /v1/merchant/orders/sync}.
 *
 * Sprint 2.1 từng dùng {@code server_time}/{@code has_more} (snake_case) theo đúng chữ trong
 * {@code docs/30-backend.md}. Sprint 2.1b đổi sang camelCase cho khớp phần còn lại của API —
 * làm lúc này vì merchant_app chưa viết, chưa có client nào phụ thuộc; sau sprint 2.2 thì
 * chi phí đổi gấp nhiều lần. {@code docs/30-backend.md} và {@code docs/90-api-contract.md}
 * đã sửa theo trong cùng commit.
 *
 * 🔴 {@code serverTime} KHÔNG phải "bây giờ". Nó là watermark đã lùi lại một biên an toàn —
 * xem {@code OrderSyncService#WATERMARK_SAFETY_MARGIN} để biết vì sao, đó là bản sửa cho
 * một lỗi mất đơn im lặng. Client dùng đúng giá trị này làm {@code since} cho lần poll sau,
 * và **không được** thay bằng đồng hồ máy mình: đồng hồ điện thoại lệch vài phút là chuyện
 * thường, lệch về phía tương lai sẽ khiến app bỏ qua vĩnh viễn các đơn trong khoảng lệch.
 */
public record OrderSyncResponseDto(
        List<OrderSummaryDto> orders,
        Instant serverTime,
        boolean hasMore) {
}
