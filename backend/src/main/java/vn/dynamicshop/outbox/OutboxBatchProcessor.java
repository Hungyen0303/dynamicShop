package vn.dynamicshop.outbox;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.dynamicshop.notification.FcmSender;

/**
 * Tách RIÊNG khỏi {@link OutboxWorker} — bean khác, gọi qua injected reference, KHÔNG phải
 * self-invocation. Spring AOP (@Transactional) chỉ áp dụng khi method được gọi XUYÊN QUA
 * proxy; gọi {@code this.method()} trong cùng class bỏ qua proxy hoàn toàn — bug thật đã
 * gặp: entity bị đổi field nhưng không transaction nào bao quanh để flush, mất thay đổi
 * trong im lặng. GUC tenant cũng gián tiếp phụ thuộc @Transactional thật sự chạy —
 * {@code TenantAwareJpaTransactionManager} set GUC ngay lúc transaction mở.
 *
 * Bất biến #7 — đây là NƠI DUY NHẤT gọi {@link FcmSender}, ngoài mọi transaction nghiệp vụ
 * tạo/đổi đơn. Dòng log {@code [outbox]} bên dưới GIỮ NGUYÊN như Stage 0 (không đổi hành vi
 * quan sát được khi chưa cấu hình gì) — lời gọi {@code fcmSender.send(...)} là đường đi THẬT
 * cho việc gửi push, cộng thêm chứ không thay thế: khi fallback (chưa có Firebase),
 * {@code LogOnlyFcmSender} tự log dòng riêng (tag {@code [fcm]}), không đụng tới dòng
 * {@code [outbox]} này.
 */
@Service
public class OutboxBatchProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxBatchProcessor.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxRepository outboxRepository;
    private final FcmSender fcmSender;

    public OutboxBatchProcessor(OutboxRepository outboxRepository, FcmSender fcmSender) {
        this.outboxRepository = outboxRepository;
        this.fcmSender = fcmSender;
    }

    @Transactional
    public void processCurrentTenantBatch() {
        List<OutboxEvent> pending = outboxRepository
                .findByProcessedAtIsNullOrderByCreatedAtAsc(Pageable.ofSize(BATCH_SIZE));
        for (OutboxEvent event : pending) {
            log.info("[outbox] tenant={} type={} aggregateId={} payload={}",
                    event.getTenantId(), event.getType(), event.getAggregateId(), event.getPayload());

            // Stage 1: chưa có bảng đăng ký device token merchant (Stage 2, merchant_app) —
            // deviceToken luôn null. FcmSender xử lý null an toàn (LogOnlyFcmSender log,
            // FirebaseFcmSender bỏ qua không gửi). Gọi ở đây, ngoài transaction tạo/đổi đơn
            // gốc — đúng bất biến #7.
            fcmSender.send(null, titleFor(event.getType()), bodyFor(event), event.getPayload());

            event.incrementAttempts();
            event.markProcessed();
        }
    }

    private String titleFor(String eventType) {
        return switch (eventType) {
            case "NEW_ORDER" -> "Đơn hàng mới";
            case "ORDER_STATUS_CHANGED" -> "Đơn hàng cập nhật";
            default -> eventType;
        };
    }

    private String bodyFor(OutboxEvent event) {
        Object code = event.getPayload().get("code");
        Object total = event.getPayload().get("total");
        return "Mã đơn %s — %s đ".formatted(code, total);
    }
}
