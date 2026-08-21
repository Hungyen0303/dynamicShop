package vn.dynamicshop.outbox;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API công khai duy nhất của package outbox/ để package khác enqueue sự kiện — không ai
 * được insert thẳng vào {@link OutboxRepository} (docs/30-backend.md: "Package không gọi
 * chéo repository của nhau").
 *
 * {@code @Transactional} với propagation mặc định (REQUIRED) — LUÔN join vào transaction
 * đang tạo/đổi thực thể của caller (ví dụ OrderService.createOrder), KHÔNG BAO GIỜ mở
 * transaction riêng, để rollback ở service gọi cũng rollback luôn outbox row.
 */
@Service
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;

    public OutboxPublisher(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public void enqueue(UUID aggregateId, String type, Map<String, Object> payload) {
        outboxRepository.save(new OutboxEvent(aggregateId, type, payload));
    }
}
