package vn.dynamicshop.order;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.dynamicshop.common.error.ApiException;

/**
 * ĐIỂM VÀO DUY NHẤT để đổi order_status hoặc payment_status — bất biến #6, #5.
 * Không {@code order.setStatus()} ở bất kỳ đâu khác trong codebase.
 */
@Component
public class OrderStateMachine {

    private final OrderEventRepository orderEventRepository;

    public OrderStateMachine(OrderEventRepository orderEventRepository) {
        this.orderEventRepository = orderEventRepository;
    }

    /**
     * Ghi dòng order_events đầu tiên lúc tạo đơn (from_status = null, to_status = PENDING) —
     * không phải "transition" theo nghĩa đổi trạng thái, nhưng vẫn qua điểm vào duy nhất
     * này để mọi thay đổi trạng thái đơn (kể cả lúc sinh ra) đều nằm trong order_events.
     */
    @Transactional
    public Order recordCreation(Order order, OrderEvent.ActorType actorType, UUID actorId) {
        orderEventRepository.save(new OrderEvent(order.getId(), null, order.getOrderStatus().name(), actorType,
                actorId, "created"));
        return order;
    }

    @Transactional
    public Order transitionOrderStatus(Order order, OrderStatus to, OrderEvent.ActorType actorType, UUID actorId,
            String reason) {
        OrderStatus from = order.getOrderStatus();
        if (!from.canTransitionTo(to)) {
            throw new InvalidOrderTransitionException(from, to);
        }
        order.applyOrderStatus(to);
        orderEventRepository.save(new OrderEvent(order.getId(), from.name(), to.name(), actorType, actorId, reason));
        return order;
    }

    @Transactional
    public Order transitionPaymentStatus(Order order, PaymentStatus to, OrderEvent.ActorType actorType, UUID actorId,
            String reason) {
        PaymentStatus from = order.getPaymentStatus();
        if (!from.canTransitionTo(to)) {
            throw new InvalidPaymentTransitionException(from, to);
        }
        order.applyPaymentStatus(to);
        // order_events dùng chung bảng cho cả hai trục, tiền tố PAYMENT: để phân biệt khi đọc lịch sử.
        orderEventRepository.save(new OrderEvent(order.getId(), "PAYMENT:" + from.name(), "PAYMENT:" + to.name(),
                actorType, actorId, reason));
        return order;
    }

    public static class InvalidOrderTransitionException extends ApiException {
        public InvalidOrderTransitionException(OrderStatus from, OrderStatus to) {
            super(HttpStatus.CONFLICT, "INVALID_ORDER_TRANSITION",
                    "Không thể chuyển order_status từ %s sang %s".formatted(from, to));
        }
    }

    public static class InvalidPaymentTransitionException extends ApiException {
        public InvalidPaymentTransitionException(PaymentStatus from, PaymentStatus to) {
            super(HttpStatus.CONFLICT, "INVALID_PAYMENT_TRANSITION",
                    "Không thể chuyển payment_status từ %s sang %s".formatted(from, to));
        }
    }
}
