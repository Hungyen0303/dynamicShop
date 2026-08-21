package vn.dynamicshop.order.dto;

import vn.dynamicshop.order.OrderItem;

/** Snapshot lúc đặt — không join products (bất biến #4). */
public record OrderItemResponseDto(String nameSnapshot, long unitPrice, int qty, long lineTotal) {

    public static OrderItemResponseDto from(OrderItem item) {
        return new OrderItemResponseDto(item.getNameSnapshot(), item.getUnitPrice(), item.getQty(),
                item.lineTotal());
    }
}
