package vn.dynamicshop.storefront;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * PK = tenant_id (quan hệ 1-1, không phải @TenantId — chính nó LÀ khoá tenant).
 * {@code layout} chỉ chứa {@code {"screens": {...}}}; {@code theme} tách cột riêng để
 * merchant app (Stage 2) có thể sửa theme mà không đụng layout. API storefront ghép cả
 * hai lại thành một object {@code {schema_version, theme, screens}} khớp
 * contracts/storefront.schema.json — xem {@code StorefrontMapper}.
 */
@Entity
@Table(name = "storefronts")
public class Storefront {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> layout;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> theme;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Storefront() {
    }

    public Storefront(UUID tenantId, int schemaVersion, Map<String, Object> layout, Map<String, Object> theme) {
        this.tenantId = tenantId;
        this.schemaVersion = schemaVersion;
        this.layout = layout;
        this.theme = theme;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public Map<String, Object> getLayout() {
        return layout;
    }

    public Map<String, Object> getTheme() {
        return theme;
    }
}
