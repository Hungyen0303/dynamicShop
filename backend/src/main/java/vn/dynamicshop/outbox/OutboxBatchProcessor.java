package vn.dynamicshop.outbox;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tách RIÊNG khỏi {@link OutboxWorker} — bean khác, gọi qua injected reference, KHÔNG phải
 * self-invocation. Spring AOP (@Transactional) chỉ áp dụng khi method được gọi XUYÊN QUA
 * proxy; gọi {@code this.method()} trong cùng class bỏ qua proxy hoàn toàn — bug thật đã
 * gặp: entity bị đổi field nhưng không transaction nào bao quanh để flush, mất thay đổi
 * trong im lặng. GUC tenant cũng gián tiếp phụ thuộc @Transactional thật sự chạy —
 * {@code TenantAwareJpaTransactionManager} set GUC ngay lúc transaction mở.
 */
@Service
public class OutboxBatchProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxBatchProcessor.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxRepository outboxRepository;

    public OutboxBatchProcessor(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public void processCurrentTenantBatch() {
        List<OutboxEvent> pending = outboxRepository
                .findByProcessedAtIsNullOrderByCreatedAtAsc(Pageable.ofSize(BATCH_SIZE));
        for (OutboxEvent event : pending) {
            // Stage 0: log thay vì gửi FCM thật — kiến trúc đúng ngay từ đầu, nối Firebase ở Stage 1.
            log.info("[outbox] tenant={} type={} aggregateId={} payload={}",
                    event.getTenantId(), event.getType(), event.getAggregateId(), event.getPayload());
            event.incrementAttempts();
            event.markProcessed();
        }
    }
}
