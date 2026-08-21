package vn.dynamicshop.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * {@code orderStatus} và {@code paymentStatus} là HAI TRỤC ĐỘC LẬP — bất biến #5.
 * KHÔNG BAO GIỜ gọi setter trạng thái trực tiếp ở đây từ bên ngoài package; luôn qua
 * {@link OrderStateMachine#transition}, nơi duy nhất ghi kèm {@code order_events}.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @UuidGenerator
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String code;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus = OrderStatus.INITIAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.INITIAL;

    @Column(nullable = false)
    private long subtotal;

    @Column(name = "shipping_fee", nullable = false)
    private long shippingFee;

    @Column(nullable = false)
    private long discount;

    @Column(nullable = false)
    private long total;

    private String note;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    private String phone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id asc")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(String code, UUID userId, String note, String deliveryAddress, String phone) {
        this.code = code;
        this.userId = userId;
        this.note = note;
        this.deliveryAddress = deliveryAddress;
        this.phone = phone;
    }

    public void addItem(OrderItem item) {
        item.attachTo(this);
        this.items.add(item);
    }

    /** Tính lại subtotal/total từ các dòng snapshot — gọi sau khi thêm hết item. */
    public void recalculateTotals(long shippingFee, long discount) {
        this.subtotal = items.stream().mapToLong(OrderItem::lineTotal).sum();
        this.shippingFee = shippingFee;
        this.discount = discount;
        this.total = this.subtotal + shippingFee - discount;
    }

    /** Chỉ {@link OrderStateMachine} được gọi — không expose setter công khai kiểu khác. */
    void applyOrderStatus(OrderStatus next) {
        this.orderStatus = next;
    }

    void applyPaymentStatus(PaymentStatus next) {
        this.paymentStatus = next;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getCode() {
        return code;
    }

    public UUID getUserId() {
        return userId;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public long getSubtotal() {
        return subtotal;
    }

    public long getShippingFee() {
        return shippingFee;
    }

    public long getDiscount() {
        return discount;
    }

    public long getTotal() {
        return total;
    }

    public String getNote() {
        return note;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getPhone() {
        return phone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
