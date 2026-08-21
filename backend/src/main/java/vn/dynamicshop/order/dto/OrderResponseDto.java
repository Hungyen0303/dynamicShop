package vn.dynamicshop.order.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.dynamicshop.order.Order;

/** DTO riêng — không serialize entity {@link Order} (bất biến #8). */
public record OrderResponseDto(
        UUID id,
        String code,
        String orderStatus,
        String paymentStatus,
        long subtotal,
        long shippingFee,
        long discount,
        long total,
        String note,
        String deliveryAddress,
        String phone,
        List<OrderItemResponseDto> items,
        Instant createdAt) {

    public static OrderResponseDto from(Order order) {
        return new OrderResponseDto(
                order.getId(),
                order.getCode(),
                order.getOrderStatus().name(),
                order.getPaymentStatus().name(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getDiscount(),
                order.getTotal(),
                order.getNote(),
                order.getDeliveryAddress(),
                order.getPhone(),
                order.getItems().stream().map(OrderItemResponseDto::from).toList(),
                order.getCreatedAt());
    }
}
