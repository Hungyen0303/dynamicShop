package vn.dynamicshop.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;

/**
 * Bất biến #7 — FCM chỉ gửi từ outbox worker, không bao giờ trong transaction nghiệp vụ.
 * Enqueue trong CÙNG transaction tạo đơn (docs/30-backend.md mục "Outbox"); gửi/log NGOÀI
 * transaction, ở {@link OutboxWorker}.
 */
@Entity
@Table(name = "outbox")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(nullable = false)
    private int attempts = 0;

    protected OutboxEvent() {
    }

    public OutboxEvent(UUID aggregateId, String type, Map<String, Object> payload) {
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
    }

    public Long getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void markProcessed() {
        this.processedAt = Instant.now();
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}
