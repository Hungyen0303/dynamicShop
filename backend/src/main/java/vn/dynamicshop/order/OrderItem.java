package vn.dynamicshop.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * SNAPSHOT — bất biến #4. {@code nameSnapshot} và {@code unitPrice} chụp lại lúc đặt,
 * KHÔNG BAO GIỜ join sang {@code products} để hiển thị đơn cũ. {@code productId} chỉ để
 * tham khảo (có thể null nếu món đã xoá), không phải quan hệ JPA — cố ý không @ManyToOne
 * sang Product để không ai lỡ tay .getProduct().getName() thay vì đọc snapshot.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "name_snapshot", nullable = false)
    private String nameSnapshot;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(nullable = false)
    private int qty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> options;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderItem() {
    }

    public OrderItem(UUID productId, String nameSnapshot, long unitPrice, int qty, Map<String, Object> options) {
        this.productId = productId;
        this.nameSnapshot = nameSnapshot;
        this.unitPrice = unitPrice;
        this.qty = qty;
        this.options = options;
    }

    void attachTo(Order order) {
        this.order = order;
    }

    public long lineTotal() {
        return unitPrice * qty;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getNameSnapshot() {
        return nameSnapshot;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public int getQty() {
        return qty;
    }

    public Map<String, Object> getOptions() {
        return options;
    }
}
