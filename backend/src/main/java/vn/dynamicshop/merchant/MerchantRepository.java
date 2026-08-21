package vn.dynamicshop.merchant;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Hibernate @TenantId tự thêm WHERE tenant_id = current — nghĩa là {@link #findByPhone}
 * chỉ tìm trong đúng tenant đang có trong {@code TenantContext} lúc gọi.
 */
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByPhone(String phone);
}
