package vn.dynamicshop.order;

import java.util.HashSet;
import java.util.Set;

/**
 * Nguồn sự thật: {@code contracts/order-states.json}. Có test drift-guard
 * ({@code OrderStateMachineContractTest}) đọc trực tiếp file JSON đó và so sánh với
 * transitions khai báo ở đây — sửa contract mà quên sửa enum này sẽ đỏ.
 *
 * Cố ý dùng {@code HashSet} thay vì {@code EnumSet.noneOf(OrderStatus.class)} trong danh
 * sách hằng số enum — gọi EnumSet.noneOf(OrderStatus.class) TRONG LÚC các hằng số enum
 * đang được khởi tạo tham chiếu ngược vào $VALUES của chính enum đó khi nó chưa đầy đủ,
 * ném ClassCastException lúc classload. An toàn ở static block bên dưới vì lúc đó $VALUES
 * đã có đủ, nhưng để nhất quán và tránh bẫy này tái diễn, dùng HashSet cho toàn bộ.
 */
public enum OrderStatus {
    PENDING(new HashSet<>()), // gán transitions ở static block, tránh forward-reference
    CONFIRMED(new HashSet<>()),
    PREPARING(new HashSet<>()),
    READY(new HashSet<>()),
    DELIVERING(new HashSet<>()),
    COMPLETED(new HashSet<>()),
    CANCELLED(new HashSet<>()),
    FAILED(new HashSet<>());

    static {
        PENDING.allowedNext.addAll(Set.of(CONFIRMED, CANCELLED));
        CONFIRMED.allowedNext.addAll(Set.of(PREPARING, CANCELLED));
        PREPARING.allowedNext.addAll(Set.of(READY, CANCELLED));
        READY.allowedNext.addAll(Set.of(DELIVERING, COMPLETED, CANCELLED));
        DELIVERING.allowedNext.addAll(Set.of(COMPLETED, FAILED));
        // COMPLETED, CANCELLED, FAILED là terminal — không thêm gì
    }

    public static final OrderStatus INITIAL = PENDING;

    private final Set<OrderStatus> allowedNext;

    OrderStatus(Set<OrderStatus> allowedNext) {
        this.allowedNext = allowedNext;
    }

    public boolean canTransitionTo(OrderStatus target) {
        return allowedNext.contains(target);
    }

    public boolean isTerminal() {
        return allowedNext.isEmpty();
    }
}
