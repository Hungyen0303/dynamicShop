package vn.dynamicshop.common.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Bất biến #9 — mọi POST tạo thực thể/đụng tiền nhận {@code Idempotency-Key}. Trùng key +
 * trùng request hash → trả lại response cũ. Trùng key + khác hash → 409.
 */
@Entity
@Table(name = "idempotency_keys")
@IdClass(IdempotencyRecordId.class)
public class IdempotencyRecord {

    @Id
    @Column(name = "key")
    private String key;

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "response_status")
    private Integer responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private Map<String, Object> responseBody;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String key, UUID tenantId, String requestHash, int responseStatus,
            Map<String, Object> responseBody) {
        this.key = key;
        this.tenantId = tenantId;
        this.requestHash = requestHash;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public Map<String, Object> getResponseBody() {
        return responseBody;
    }
}
