package vn.dynamicshop.order.dto;

import java.time.Instant;
import java.util.UUID;
import vn.dynamicshop.order.Order;

/**
 * Bản TÓM TẮT cho danh sách đơn của merchant — cố ý KHÔNG có {@code items}.
 *
 * Lý do là ngân sách byte, không phải thẩm mỹ: {@code GET /v1/merchant/orders/sync} là
 * endpoint bị gọi nhiều nhất hệ thống (poll 15–20s mỗi máy, cả ngày, trên 3G/4G ở tỉnh —
 * docs/30-backend.md "giữ nó rẻ"). Kèm cả dòng món vào mỗi đơn sẽ nhân đôi/ba kích thước
 * phản hồi cho dữ liệu mà màn hình danh sách không hiển thị. Chi tiết món lấy riêng khi
 * merchant mở một đơn cụ thể.
 *
 * {@code itemCount} truyền vào từ ngoài chứ không đọc {@code order.getItems().size()} —
 * {@code items} là {@code LAZY}, chạm vào nó cho mỗi đơn sẽ thành N+1 query đúng trên
 * endpoint nóng nhất. {@code OrderSyncService} đếm bằng MỘT query gộp cho cả trang.
 */
public record OrderSummaryDto(
        UUID id,
        String code,
        String orderStatus,
        String paymentStatus,
        long total,
        String phone,
        String deliveryAddress,
        String note,
        int itemCount,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderSummaryDto from(Order order, int itemCount) {
        return new OrderSummaryDto(
                order.getId(),
                order.getCode(),
                order.getOrderStatus().name(),
                order.getPaymentStatus().name(),
                order.getTotal(),
                order.getPhone(),
                order.getDeliveryAddress(),
                order.getNote(),
                itemCount,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
