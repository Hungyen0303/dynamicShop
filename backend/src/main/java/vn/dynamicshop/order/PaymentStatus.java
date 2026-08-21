package vn.dynamicshop.order;

import java.util.HashSet;
import java.util.Set;

/**
 * TÁCH RỜI HOÀN TOÀN {@link OrderStatus} — bất biến #5. Nguồn sự thật:
 * {@code contracts/order-states.json#/payment_status}.
 *
 * Dùng {@code HashSet} thay vì {@code EnumSet.noneOf(...)} trong constructor — xem
 * comment trong {@link OrderStatus} (gọi EnumSet.noneOf lúc enum constant đang khởi tạo
 * gây ClassCastException do $VALUES chưa đầy đủ).
 */
public enum PaymentStatus {
    UNPAID(new HashSet<>()),
    PARTIAL(new HashSet<>()),
    PAID(new HashSet<>()),
    REFUNDED(new HashSet<>());

    static {
        UNPAID.allowedNext.addAll(Set.of(PAID, PARTIAL));
        PARTIAL.allowedNext.addAll(Set.of(PAID, REFUNDED));
        PAID.allowedNext.addAll(Set.of(REFUNDED));
        // REFUNDED là terminal
    }

    public static final PaymentStatus INITIAL = UNPAID;

    private final Set<PaymentStatus> allowedNext;

    PaymentStatus(Set<PaymentStatus> allowedNext) {
        this.allowedNext = allowedNext;
    }

    public boolean canTransitionTo(PaymentStatus target) {
        return allowedNext.contains(target);
    }
}
