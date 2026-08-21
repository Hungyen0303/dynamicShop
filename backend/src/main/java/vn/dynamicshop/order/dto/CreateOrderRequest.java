package vn.dynamicshop.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty List<@Valid CreateOrderItemRequest> items,
        String note,
        String deliveryAddress,
        String phone) {
}
