package vn.dynamicshop.common.tenant;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository DUY NHẤT được phép resolve slug → tenant_id (dùng bởi
 * {@link PublicTenantSlugFilter}). Bảng tenants không có RLS nên không cần TenantContext
 * để đọc — đây chính là bảng "gốc" phá vòng lặp con-gà-quả-trứng.
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);
}
