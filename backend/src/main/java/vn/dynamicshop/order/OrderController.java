package vn.dynamicshop.order;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.dynamicshop.common.idempotency.IdempotencyService;
import vn.dynamicshop.order.dto.CreateOrderRequest;
import vn.dynamicshop.order.dto.OrderResponseDto;

/**
 * Public plane — tenant từ slug (đã resolve bởi filter). Bất biến #9: nhận
 * {@code Idempotency-Key}, gửi lại đúng request → trả lại đúng đơn cũ, KHÔNG tạo đơn thứ hai
 * (docs/70-stages.md bước 7 của flow đầu-cuối).
 */
@RestController
@RequestMapping("/v1/s/{slug}/orders")
public class OrderController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    public OrderController(OrderService orderService, IdempotencyService idempotencyService) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@PathVariable String slug,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        return idempotencyService.execute(idempotencyKey, request, OrderResponseDto.class,
                () -> ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request)));
    }
}
