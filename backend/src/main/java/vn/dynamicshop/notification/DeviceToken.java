package vn.dynamicshop.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * FCM registration token của một máy merchant. Đăng ký bởi {@code merchant_app} sau khi
 * đăng nhập, thu hồi khi đăng xuất.
 *
 * {@code revokedAt} là soft delete có chủ ý: giữ lại dòng để sau này còn tra được "máy nào
 * từng nhận đơn" khi chủ quán báo mất đơn — mất đơn là sự cố nghiêm trọng nhất của hệ thống
 * này (docs/11-merchant-app.md), điều tra nó cần lịch sử chứ không chỉ trạng thái hiện tại.
 */
@Entity
@Table(name = "device_tokens")
public class DeviceToken {

    @Id
    @UuidGenerator
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false, updatable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeviceToken() {
    }

    public DeviceToken(UUID merchantId, String token, Platform platform, String appVersion) {
        this.merchantId = merchantId;
        this.token = token;
        this.platform = platform;
        this.appVersion = appVersion;
    }

    /**
     * Đăng ký lại cùng token trên cùng tenant — máy cũ, có thể là người khác đăng nhập hoặc
     * app vừa cập nhật. Ghi đè chủ sở hữu và "sống lại" nếu trước đó đã thu hồi, thay vì tạo
     * dòng thứ hai (unique (tenant_id, token) sẽ chặn).
     */
    public void refresh(UUID merchantId, Platform platform, String appVersion) {
        this.merchantId = merchantId;
        this.platform = platform;
        this.appVersion = appVersion;
        this.lastSeenAt = Instant.now();
        this.revokedAt = null;
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public String getToken() {
        return token;
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public enum Platform {
        ANDROID, IOS
    }
}
