package vn.dynamicshop.common.tenant;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 🔴 Sửa sau review: bản trước dùng Spring AOP thuần ({@code TenantGucInterceptor} +
 * {@code TenantGucAdvisorConfig}, đã xoá) để bọc quanh "mọi method có @Transactional".
 * Cách đó có lỗ hổng thật: Spring Data JPA tự dựng proxy cho repository qua
 * {@code RepositoryFactorySupport}, KHÔNG đi qua {@code InfrastructureAdvisorAutoProxyCreator}
 * — advisor tuỳ biến không bao giờ áp dụng cho repository, chỉ áp dụng cho bean @Transactional
 * do ta tự viết (service). Gọi thẳng {@code repository.save()} mà không qua một method
 * @Transactional của service (test cô lập tenant làm vậy để seed dữ liệu — và về nguyên tắc,
 * bất kỳ code nào sau này lỡ làm vậy) khiến GUC không bao giờ được set: RLS đúng đắn từ chối
 * ghi, hoặc tệ hơn với đọc — GUC NULL khiến RLS lọc rỗng toàn bộ thay vì báo lỗi rõ ràng.
 *
 * Sửa: override {@link #doBegin} thay vì dùng AOP pointcut. {@code doBegin} chạy đúng MỘT
 * LẦN mỗi khi Spring thực sự mở một transaction VẬT LÝ mới — bất kể lời gọi bắt nguồn từ
 * service hay từ gọi thẳng repository — vì toàn bộ app chỉ dùng chung một
 * {@link org.springframework.transaction.PlatformTransactionManager} (bean này, đăng ký ở
 * {@link HibernateTenantConfig}, thay thế bean {@code transactionManager} Boot tự
 * autoconfigure). Với propagation REQUIRED tham gia transaction có sẵn, {@code doBegin} không
 * chạy lại — tự nhiên đạt đúng "set GUC một lần mỗi transaction" mà không cần tự theo dõi
 * resource key như bản cũ.
 */
public class TenantAwareJpaTransactionManager extends JpaTransactionManager {

    public TenantAwareJpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);
        TenantContext.current().ifPresent(this::applyTenantGuc);
    }

    private void applyTenantGuc(java.util.UUID tenantId) {
        var holder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(getEntityManagerFactory());
        holder.getEntityManager()
                .createNativeQuery("select set_config('app.tenant_id', ?1, true)")
                .setParameter(1, tenantId.toString())
                .getSingleResult();
    }
}
