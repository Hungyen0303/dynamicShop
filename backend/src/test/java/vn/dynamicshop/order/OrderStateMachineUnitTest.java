package vn.dynamicshop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * docs/50-qa.md mục "State machine": mọi transition hợp lệ + mọi transition không hợp lệ
 * bị từ chối. Test thuần (mock repository) — không cần DB, bù cho
 * {@link OrderStateMachineContractTest} vốn chỉ kiểm tra bảng transitions, không kiểm tra
 * OrderStateMachine THỰC SỰ ghi order_events.
 */
class OrderStateMachineUnitTest {

    private final OrderEventRepository orderEventRepository = mock(OrderEventRepository.class);
    private final OrderStateMachine stateMachine = new OrderStateMachine(orderEventRepository);

    @Test
    void transition_hop_le_doi_trang_thai_va_ghi_order_event() {
        Order order = newOrderWithId();

        stateMachine.transitionOrderStatus(order, OrderStatus.CONFIRMED, OrderEvent.ActorType.MERCHANT,
                UUID.randomUUID(), "xác nhận đơn");

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderEventRepository).save(org.mockito.ArgumentMatchers.argThat(
                event -> "PENDING".equals(event.getFromStatus()) && "CONFIRMED".equals(event.getToStatus())));
    }

    @Test
    void transition_khong_hop_le_bi_tu_choi_va_khong_ghi_gi_ca() {
        Order order = newOrderWithId(); // PENDING

        assertThatThrownBy(() -> stateMachine.transitionOrderStatus(order, OrderStatus.COMPLETED,
                OrderEvent.ActorType.MERCHANT, UUID.randomUUID(), null))
                .isInstanceOf(OrderStateMachine.InvalidOrderTransitionException.class);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING); // không đổi
        verify(orderEventRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void terminal_status_khong_the_chuyen_di_dau_nua() {
        Order order = newOrderWithId();
        stateMachine.transitionOrderStatus(order, OrderStatus.CANCELLED, OrderEvent.ActorType.CUSTOMER, null, null);

        assertThatThrownBy(() -> stateMachine.transitionOrderStatus(order, OrderStatus.CONFIRMED,
                OrderEvent.ActorType.MERCHANT, UUID.randomUUID(), null))
                .isInstanceOf(OrderStateMachine.InvalidOrderTransitionException.class);
    }

    @Test
    void payment_status_transition_hop_le() {
        Order order = newOrderWithId();
        stateMachine.transitionPaymentStatus(order, PaymentStatus.PAID, OrderEvent.ActorType.MERCHANT,
                UUID.randomUUID(), "khách chuyển khoản");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void payment_status_transition_khong_hop_le_bi_tu_choi() {
        Order order = newOrderWithId(); // UNPAID
        assertThatThrownBy(() -> stateMachine.transitionPaymentStatus(order, PaymentStatus.REFUNDED,
                OrderEvent.ActorType.MERCHANT, UUID.randomUUID(), null))
                .isInstanceOf(OrderStateMachine.InvalidPaymentTransitionException.class);
    }

    private Order newOrderWithId() {
        Order order = new Order("OD-TEST", null, null, null, null);
        setId(order, UUID.randomUUID());
        return order;
    }

    private void setId(Order order, UUID id) {
        try {
            var field = Order.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(order, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
