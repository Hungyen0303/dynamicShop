package vn.dynamicshop.order;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.dynamicshop.common.error.ApiException;
import vn.dynamicshop.order.dto.OrderResponseDto;
import vn.dynamicshop.order.dto.TransitionOrderStatusRequest;

/**
 * Authenticated plane — tenant từ JWT claim (đã set bởi {@code JwtAuthenticationFilter}).
 * Đây là bước 6 của flow đầu-cuối Stage 0 ("Gọi API xác nhận đơn → trạng thái đổi,
 * order_events thêm dòng").
 */
@RestController
@RequestMapping("/v1/merchant/orders")
public class MerchantOrderController {

    private final OrderService orderService;

    public MerchantOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{id}/transition")
    public OrderResponseDto transition(@PathVariable UUID id, @Valid @RequestBody TransitionOrderStatusRequest request,
            Authentication authentication) {
        OrderStatus to = parseStatus(request.to());
        UUID merchantId = UUID.fromString(authentication.getName());
        return orderService.transitionStatus(id, to, OrderEvent.ActorType.MERCHANT, merchantId, request.reason());
    }

    private OrderStatus parseStatus(String raw) {
        try {
            return OrderStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusValueException(raw);
        }
    }

    public static class InvalidStatusValueException extends ApiException {
        public InvalidStatusValueException(String raw) {
            super(HttpStatus.BAD_REQUEST, "INVALID_STATUS_VALUE", "order_status không hợp lệ: " + raw);
        }
    }
}
