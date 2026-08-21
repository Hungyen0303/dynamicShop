package vn.dynamicshop.common.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 🔴 Trái tim của cô lập tenant ở tầng DB — phát {@code SET LOCAL app.tenant_id}
 * (qua {@code set_config(..., true)}, tham số hoá — KHÔNG string-concat, tránh SQL
 * injection dù ví dụ trong docs/31-database.md viết bằng string nối chuỗi).
 *
 * Chạy như một Advisor thường của Spring AOP (không cần AspectJ/spring-boot-starter-aop —
 * dependency đó chưa được duyệt). Được {@link TenantGucAdvisorConfig} bọc quanh MỌI method
 * có @Transactional (service của ta + repository của Spring Data JPA), và nhờ
 * {@link TransactionOrderConfig} đẩy advisor @Transactional ra ngoài cùng, method này luôn
 * chạy SAU khi transaction vật lý đã bắt đầu.
 *
 * LUÔN dùng SET LOCAL (qua set_config is_local=true) — KHÔNG BAO GIỜ SET. SET LOCAL tự hết
 * hiệu lực khi transaction kết thúc (commit hoặc rollback), nên khi HikariCP trả connection
 * về pool, GUC luôn sạch — đó là cách duy nhất tránh rò rỉ tenant qua connection pool.
 */
@Component
public class TenantGucInterceptor implements MethodInterceptor {

    /** Khoá đánh dấu "đã set GUC cho transaction hiện tại" trong TransactionSynchronizationManager. */
    private static final Object GUC_APPLIED_RESOURCE_KEY = new Object();

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            applyTenantGucOnce();
        }
        return invocation.proceed();
    }

    private void applyTenantGucOnce() {
        if (TransactionSynchronizationManager.hasResource(GUC_APPLIED_RESOURCE_KEY)) {
            return; // đã set cho transaction này rồi — tránh gọi lặp lại ở mọi nested @Transactional
        }
        TenantContext.current().ifPresent(tenantId -> {
            entityManager
                    .createNativeQuery("select set_config('app.tenant_id', ?1, true)")
                    .setParameter(1, tenantId.toString())
                    .getSingleResult();
            TransactionSynchronizationManager.bindResource(GUC_APPLIED_RESOURCE_KEY, Boolean.TRUE);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (TransactionSynchronizationManager.hasResource(GUC_APPLIED_RESOURCE_KEY)) {
                        TransactionSynchronizationManager.unbindResource(GUC_APPLIED_RESOURCE_KEY);
                    }
                }
            });
        });
    }
}
