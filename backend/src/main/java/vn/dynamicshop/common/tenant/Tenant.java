package vn.dynamicshop.common.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Bảng gốc — KHÔNG có tenant_id, KHÔNG bật RLS (xem V1__init.sql). Mọi tenant khác trỏ
 * về đây qua khoá ngoại, và {@code slug} là lối vào public plane (/v1/s/{slug}/...).
 */
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(nullable = false)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "business_day_start", nullable = false)
    private LocalTime businessDayStart = LocalTime.of(4, 0);

    @Column(name = "storage_quota_bytes", nullable = false)
    private long storageQuotaBytes = 2_147_483_648L;

    @Column(name = "storage_used_bytes", nullable = false)
    private long storageUsedBytes = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Tenant() {
        // JPA
    }

    public Tenant(String slug, String name) {
        this.slug = slug;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public String getTimezone() {
        return timezone;
    }

    public LocalTime getBusinessDayStart() {
        return businessDayStart;
    }

    public enum TenantStatus {
        ACTIVE, SUSPENDED
    }
}
