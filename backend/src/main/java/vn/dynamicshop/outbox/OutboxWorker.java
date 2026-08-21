package vn.dynamicshop.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.dynamicshop.common.tenant.Tenant;
import vn.dynamicshop.common.tenant.TenantContext;
import vn.dynamicshop.common.tenant.TenantRepository;

/**
 * 🔴 Stage 0: chỉ GHI LOG, chưa nối FCM (chưa có Firebase — docs/70-stages.md). Nối FCM
 * thật ở Stage 1, trong {@code notification/}. Bất biến #7: FCM chỉ được gửi từ đây,
 * không bao giờ trong transaction nghiệp vụ tạo/đổi đơn.
 *
 * Duyệt TỪNG TENANT một, set {@link TenantContext} cho mỗi vòng — outbox có
 * {@code @TenantId} nên phải có tenant hiện tại mới query đúng được; đây là cách xử lý
 * cross-tenant mà KHÔNG cần role BYPASSRLS (role đó chỉ dành riêng cho package admin/).
 * Chấp nhận được ở quy mô vài tenant Stage 0; nếu số tenant lớn, đổi sang cách khác
 * (batch theo tenant_id) ở Stage sau.
 *
 * Việc xử lý thật nằm ở {@link OutboxBatchProcessor} — bean RIÊNG, gọi qua injected
 * reference chứ không phải self-invocation, để @Transactional áp dụng đúng (self-invocation
 * bỏ qua toàn bộ proxy AOP của Spring) — và nhờ đó GUC tenant cũng được set đúng lúc, vì
 * {@code TenantAwareJpaTransactionManager} set GUC ngay khi transaction mở.
 */
@Component
public class OutboxWorker {

    private final TenantRepository tenantRepository;
    private final OutboxBatchProcessor batchProcessor;

    public OutboxWorker(TenantRepository tenantRepository, OutboxBatchProcessor batchProcessor) {
        this.tenantRepository = tenantRepository;
        this.batchProcessor = batchProcessor;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:5000}")
    public void pollAndLog() {
        for (Tenant tenant : tenantRepository.findAll()) {
            TenantContext.set(tenant.getId());
            try {
                batchProcessor.processCurrentTenantBatch();
            } finally {
                TenantContext.clear();
            }
        }
    }
}
