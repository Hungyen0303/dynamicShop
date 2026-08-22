package vn.dynamicshop.order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /** {@code @Transactional} bắt buộc cho query dẫn xuất — xem Javadoc {@code DeviceTokenRepository}. */
    @Transactional(readOnly = true)
    Optional<Order> findByCode(String code);

    /**
     * Delta cho {@code GET /v1/merchant/orders/sync}. Dựa thẳng vào index
     * {@code (tenant_id, updated_at)} có sẵn từ V1 — đúng index mà V1 ghi chú là "quan
     * trọng nhất, endpoint sync merchant dựa vào nó".
     *
     * {@code GreaterThanEqual} chứ không phải {@code GreaterThan}: nhiều đơn có thể trùng
     * {@code updated_at} tới từng micro giây (một lượt cập nhật hàng loạt), và nếu trang
     * kết thúc giữa một nhóm trùng mốc thì {@code >} sẽ nhảy qua phần còn lại của nhóm —
     * mất đơn trong im lặng. Đổi lại, client nhận lặp phần rìa và phải dedupe theo
     * {@code id}; nó vốn đã bắt buộc phải dedupe vì push là at-least-once (bất biến #4,
     * docs/11-merchant-app.md), nên đây không phải yêu cầu mới.
     *
     * Sắp xếp phụ theo {@code id} để thứ tự ổn định giữa các lần gọi khi trùng
     * {@code updated_at} — không có nó, phân trang trên nhóm trùng mốc là ngẫu nhiên.
     */
    @Transactional(readOnly = true)
    List<Order> findByUpdatedAtGreaterThanEqualOrderByUpdatedAtAscIdAsc(Instant since, Pageable pageable);
}
