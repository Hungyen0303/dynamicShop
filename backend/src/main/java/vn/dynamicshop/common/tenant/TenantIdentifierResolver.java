package vn.dynamicshop.common.tenant;

import java.util.UUID;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Tầng 1 của cô lập tenant — Hibernate {@code @TenantId} (docs/31-database.md,
 * ".claude/skills/tenant-isolation"). Mọi entity có field @TenantId sẽ tự động được
 * Hibernate thêm "WHERE tenant_id = :tenantId" vào mọi query, và tự gán lúc insert.
 *
 * Đây là lớp "nhanh, tự động, app-level, cứu 99%". RLS ở Postgres (V1__init.sql) là
 * lớp thứ hai, "người từ chối CUỐI CÙNG" — cứu 1% còn lại, kể cả khi ai đó quên set
 * TenantContext hoặc viết native query.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

    /** Sentinel dùng khi không có tenant nào trong context (ví dụ bảng toàn cục). */
    private static final UUID NO_TENANT = new UUID(0L, 0L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.current().orElse(NO_TENANT);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
