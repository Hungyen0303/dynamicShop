package vn.dynamicshop.order;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.dynamicshop.order.dto.OrderSummaryDto;
import vn.dynamicshop.order.dto.OrderSyncResponseDto;

/**
 * Nguồn dữ liệu cho kênh nhận đơn FOREGROUND của merchant_app (bất biến #1,
 * docs/11-merchant-app.md: hai kênh, không tin vào một cái nào). Kênh này KHÔNG phụ thuộc
 * Firebase một dòng nào — đó là lý do sprint 2.1 làm được đầy đủ trong khi credential FCM
 * thật vẫn còn treo (progress.md mục 9).
 *
 * Ba ràng buộc chi phối toàn bộ thiết kế ở đây, tất cả đều là ngân sách chứ không phải sở
 * thích — endpoint này bị gọi nhiều nhất hệ thống (docs/30-backend.md "giữ nó rẻ"):
 *   1. Trang có trần cứng {@link #MAX_PAGE_SIZE}, client không đổi được.
 *   2. Trả DTO tóm tắt, không kèm dòng món.
 *   3. Có ETag → lần poll không có gì mới tốn ~200 byte thay vì cả trang JSON.
 */
@Service
public class OrderSyncService {

    /**
     * Trần cứng, KHÔNG cho client tự chọn. Cho phép {@code ?limit=} là mở đường để một máy
     * cấu hình sai kéo cả nghìn đơn mỗi 15 giây trên 4G — trần do server giữ thì sai lầm
     * của một máy không thành sự cố của cả hệ thống.
     */
    private static final int MAX_PAGE_SIZE = 50;

    /**
     * 🔴 Biên an toàn trừ vào mốc {@code serverTime} trả cho client. Đây là bản sửa cho một
     * lỗi mất đơn IM LẶNG đã lọt qua sprint 2.1 — đọc kỹ trước khi ai đó thấy nó thừa và
     * xoá đi.
     *
     * {@code Order.updatedAt} do Hibernate {@code @UpdateTimestamp} gán lúc FLUSH, nhưng
     * transaction commit SAU đó. Nên tồn tại cửa sổ mà một đơn đã mang {@code updated_at =
     * T1} nhưng chưa nhìn thấy được từ transaction khác. Nếu client lấy {@code since} cho
     * lần poll sau bằng thời điểm đọc xong (T2 > T1), đơn đó rơi vào khe: nó không có trong
     * kết quả lần này (chưa commit), và lần sau bị lọc mất vì {@code T1 < T2}. **Không bao
     * giờ xuất hiện lại, không log, không lỗi** — đúng thứ mà cả sản phẩm này tồn tại để
     * tránh (docs/11-merchant-app.md: "Không bao giờ sót đơn").
     *
     * Sửa bằng cách lùi mốc về quá khứ xa hơn mọi transaction có thể kéo dài. Cái giá là
     * client nhận lại các đơn trong cửa sổ 60 giây ở lần poll kế tiếp — vô hại, vì client
     * BẮT BUỘC phải dedupe theo {@code id} rồi (push at-least-once, bất biến #4), và ETag
     * vẫn cho {@code 304} bình thường khi không có gì đổi.
     *
     * 60 giây là rộng rãi so với transaction thật của app (mili giây). Có thể siết lại nếu
     * đo được, nhưng siết quá tay thì lỗi quay lại ở dạng im lặng — không đáng đánh đổi.
     */
    private static final Duration WATERMARK_SAFETY_MARGIN = Duration.ofSeconds(60);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderSyncService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * @param since mốc {@code updated_at} client đã đồng bộ tới; {@code null} = đồng bộ đầy
     *              đủ từ đầu (lần cài app đầu tiên, hoặc sau khi xoá dữ liệu app).
     */
    @Transactional(readOnly = true)
    public OrderSyncResponseDto sync(Instant since) {
        Instant from = since == null ? Instant.EPOCH : since;

        // 🔴 Watermark phải lấy TRƯỚC khi query, và lùi thêm một biên an toàn. Xem
        // WATERMARK_SAFETY_MARGIN — đây là dòng đứng giữa "đồng bộ đúng" và "mất đơn
        // vĩnh viễn", không phải chi tiết vặt về thứ tự lệnh.
        Instant watermark = Instant.now().minus(WATERMARK_SAFETY_MARGIN);

        // Lấy dư MỘT dòng để biết còn trang sau hay không, thay vì chạy thêm một câu COUNT
        // trên toàn bộ delta — count đắt hơn hẳn và chỉ để trả về đúng một chữ true/false.
        List<Order> page = orderRepository.findByUpdatedAtGreaterThanEqualOrderByUpdatedAtAscIdAsc(
                from, Pageable.ofSize(MAX_PAGE_SIZE + 1));

        boolean hasMore = page.size() > MAX_PAGE_SIZE;
        List<Order> orders = hasMore ? page.subList(0, MAX_PAGE_SIZE) : page;

        Map<UUID, Integer> itemCounts = countItems(orders);
        List<OrderSummaryDto> summaries = orders.stream()
                .map(order -> OrderSummaryDto.from(order, itemCounts.getOrDefault(order.getId(), 0)))
                .toList();

        return new OrderSyncResponseDto(summaries, watermark, hasMore);
    }

    /**
     * ETag yếu tính từ (id, updated_at) của đúng trang sắp trả về. Không dùng
     * count+max(updated_at) cho rẻ: hai trang khác nội dung vẫn có thể trùng cặp số đó khi
     * một đơn bị sửa rồi một đơn khác được sửa lại về cùng mốc, và khi ấy client sẽ nhận
     * {@code 304} cho dữ liệu đã cũ — mất cập nhật đơn trong im lặng.
     *
     * Danh sách rỗng băm ra một giá trị cố định, nên chuỗi ngày dài không có đơn nào (ban
     * đêm) là chuỗi {@code 304} liên tiếp, đúng mục tiêu "phần lớn lần poll ~200 byte".
     */
    public String etagFor(OrderSyncResponseDto response) {
        // Độ phân giải nano, không phải milli: Postgres lưu timestamptz tới micro giây, và
        // hai lần sửa cùng một đơn trong cùng một milli giây (nhân viên bấm nhanh, hoặc một
        // lượt cập nhật hàng loạt) sẽ băm ra cùng ETag nếu cắt ở milli — client nhận 304 cho
        // dữ liệu đã cũ, mất cập nhật trong im lặng.
        String fingerprint = response.orders().stream()
                .map(order -> order.id() + ":" + order.updatedAt().getEpochSecond() + "."
                        + order.updatedAt().getNano())
                .collect(Collectors.joining("|"));
        return "\"" + sha256(fingerprint).substring(0, 32) + "\"";
    }

    private Map<UUID, Integer> countItems(List<Order> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<UUID> orderIds = orders.stream().map(Order::getId).toList();
        // group by trong query đảm bảo mỗi orderId xuất hiện đúng một lần → không cần merge function.
        return orderItemRepository.countByOrderIds(orderIds).stream()
                .collect(Collectors.toMap(OrderItemRepository.OrderItemCount::getOrderId,
                        row -> Math.toIntExact(row.getCnt())));
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
