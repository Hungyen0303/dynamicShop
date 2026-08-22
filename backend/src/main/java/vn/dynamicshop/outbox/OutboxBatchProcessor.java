package vn.dynamicshop.outbox;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.dynamicshop.notification.DeviceTokenService;
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
 * quan sát được khi chưa cấu hình gì) — khi fallback (chưa có Firebase),
 * {@code LogOnlyFcmSender} tự log dòng riêng (tag {@code [fcm]}), không đụng tới dòng
 * {@code [outbox]} này.
 *
 * 🔴 Điểm dễ rò rỉ nhất trong sprint 2.1: token được lấy TRONG transaction đã có GUC tenant
 * của vòng lặp hiện tại ({@link OutboxWorker} set {@code TenantContext} cho từng tenant),
 * nên {@code device_tokens} đã bị Hibernate {@code @TenantId} lọc và RLS chặn lần cuối. Gửi
 * đơn của shop này sang máy của shop khác là loại lỗi mất cả tỉnh, nên nó có test riêng
 * ({@code OutboxFcmFanoutTest#khong_gui_push_sang_thiet_bi_cua_tenant_khac}) chứ không chỉ
 * dựa vào việc đọc code thấy đúng.
 */
@Service
public class OutboxBatchProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxBatchProcessor.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxRepository outboxRepository;
    private final FcmSender fcmSender;
    private final DeviceTokenService deviceTokenService;

    public OutboxBatchProcessor(OutboxRepository outboxRepository, FcmSender fcmSender,
            DeviceTokenService deviceTokenService) {
        this.outboxRepository = outboxRepository;
        this.fcmSender = fcmSender;
        this.deviceTokenService = deviceTokenService;
    }

    @Transactional
    public void processCurrentTenantBatch() {
        List<OutboxEvent> pending = outboxRepository
                .findByProcessedAtIsNullOrderByCreatedAtAsc(Pageable.ofSize(BATCH_SIZE));
        if (pending.isEmpty()) {
            return;
        }

        // Lấy một lần cho cả batch, không phải mỗi event — batch thường là nhiều đơn của
        // CÙNG một tenant, và danh sách máy của một quán gần như không đổi trong vài giây.
        List<String> tokens = deviceTokenService.activeTokensForCurrentTenant();

        for (OutboxEvent event : pending) {
            log.info("[outbox] tenant={} type={} aggregateId={} payload={}",
                    event.getTenantId(), event.getType(), event.getAggregateId(), event.getPayload());

            if (tokens.isEmpty()) {
                // Không phải lỗi: quán chưa cài merchant_app, hoặc đã đăng xuất khỏi mọi máy.
                // Vẫn đánh dấu processed — giữ push lại để "gửi sau" là vô nghĩa với thông báo
                // đơn hàng, đơn cũ 20 phút không còn đáng kêu chuông.
                log.info("[fcm] bỏ qua — tenant={} chưa có device token nào còn sống", event.getTenantId());
            }
            for (String token : tokens) {
                fcmSender.send(token, titleFor(event.getType()), bodyFor(event), event.getPayload());
            }

            event.incrementAttempts();
            event.markProcessed();
        }
    }

    private String titleFor(String eventType) {
        return switch (eventType) {
            case "NEW_ORDER" -> "Đơn hàng mới";
            case "ORDER_STATUS_CHANGED" -> "Đơn hàng cập nhật";
            case "ORDER_PAYMENT_CHANGED" -> "Cập nhật thanh toán";
            default -> eventType;
        };
    }

    private String bodyFor(OutboxEvent event) {
        Object code = event.getPayload().get("code");
        Object total = event.getPayload().get("total");
        return "Mã đơn %s — %s đ".formatted(code, total);
    }
}
