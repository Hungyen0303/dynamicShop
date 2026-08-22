package vn.dynamicshop.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mọi query ở đây đã được Hibernate {@code @TenantId} lọc theo tenant hiện tại, và Postgres
 * RLS chặn lần cuối (policy {@code tenant_isolation} trên {@code device_tokens}, V2). Không
 * có method nào nhận {@code tenantId} làm tham số — tenant CHỈ đến từ
 * {@code TenantContext}, không bao giờ từ lời gọi (bất biến #1).
 *
 * 🔴 {@code @Transactional} trên TỪNG method dẫn xuất, không phải trang trí thừa. Bẫy thật
 * gặp lúc viết sprint 2.1: {@code SimpleJpaRepository} có {@code @Transactional(readOnly)}
 * ở mức class, nhưng nó CHỈ phủ các method kế thừa ({@code save}, {@code findById},
 * {@code findAll}) — query dẫn xuất khai báo ở interface này thì KHÔNG. Không có
 * transaction thì {@code TenantAwareJpaTransactionManager.doBegin()} không chạy, GUC
 * {@code app.tenant_id} không bao giờ được set, và policy RLS đánh giá trên một GUC rỗng.
 * Gọi từ trong một service {@code @Transactional} thì không lộ (transaction đã có sẵn) —
 * đúng loại lỗi ẩn mà {@code TenantAwareJpaTransactionManager} được viết ra để chống.
 *
 * Đặt trên từng method chứ KHÔNG đặt {@code @Transactional(readOnly = true)} ở mức
 * interface: mức interface sẽ phủ cả {@code save()} kế thừa và biến mọi lệnh ghi thành
 * read-only.
 */
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    @Transactional(readOnly = true)
    Optional<DeviceToken> findByToken(String token);

    @Transactional(readOnly = true)
    List<DeviceToken> findByRevokedAtIsNull();
}
