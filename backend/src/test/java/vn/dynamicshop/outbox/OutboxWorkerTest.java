package vn.dynamicshop.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.dynamicshop.AbstractIntegrationTest;
import vn.dynamicshop.common.tenant.Tenant;
import vn.dynamicshop.common.tenant.TenantContext;
import vn.dynamicshop.common.tenant.TenantRepository;

/**
 * Bất biến #7 — outbox worker Stage 0 chỉ ghi log (chưa nối FCM, chưa có Firebase). Test
 * xác nhận event enqueue được worker nhặt lên và đánh dấu processed — chứng minh vòng đời
 * outbox hoạt động dù chưa gửi thật.
 */
class OutboxWorkerTest extends AbstractIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private OutboxPublisher outboxPublisher;
    @Autowired
    private OutboxRepository outboxRepository;
    @Autowired
    private OutboxWorker outboxWorker;

    @Test
    void worker_danh_dau_processed_sau_khi_log() {
        Tenant tenant = tenantRepository.save(new Tenant("outbox-test-" + UUID.randomUUID(), "Outbox test"));
        UUID aggregateId = UUID.randomUUID();

        TenantContext.set(tenant.getId());
        try {
            outboxPublisher.enqueue(aggregateId, "NEW_ORDER", Map.of("orderId", aggregateId.toString()));
        } finally {
            TenantContext.clear();
        }

        outboxWorker.pollAndLog();

        TenantContext.set(tenant.getId());
        try {
            var events = outboxRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getProcessedAt()).isNotNull();
            assertThat(events.get(0).getAttempts()).isEqualTo(1);
        } finally {
            TenantContext.clear();
        }
    }
}
