package vn.dynamicshop.order;

import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.dynamicshop.common.error.ApiException;
import vn.dynamicshop.common.idempotency.IdempotencyService;
import vn.dynamicshop.order.dto.MarkPaymentRequest;
import vn.dynamicshop.order.dto.OrderResponseDto;
import vn.dynamicshop.order.dto.OrderSyncResponseDto;
import vn.dynamicshop.order.dto.TransitionOrderStatusRequest;

/**
 * Authenticated plane — tenant từ JWT claim (đã set bởi {@code JwtAuthenticationFilter}).
 * Không route nào ở đây nhận tenant qua path/body: {@code {id}} là orderId, và một orderId
 * của tenant khác sẽ ra {@code 404 ORDER_NOT_FOUND} chứ không phải {@code 403} — không xác
 * nhận cho người gọi biết đơn đó có tồn tại ở đâu đó hay không.
 */
@RestController
@RequestMapping("/v1/merchant/orders")
public class MerchantOrderController {

    private final OrderService orderService;
    private final OrderSyncService orderSyncService;
    private final IdempotencyService idempotencyService;

    public MerchantOrderController(OrderService orderService, OrderSyncService orderSyncService,
            IdempotencyService idempotencyService) {
        this.orderService = orderService;
        this.orderSyncService = orderSyncService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Kênh nhận đơn foreground của merchant_app — poll mỗi 15–20s. KHÔNG cần
     * {@code Idempotency-Key}: đây là GET, không tạo gì.
     *
     * {@code If-None-Match} → {@code 304} khi trang không đổi. merchant_app phải gửi lại
     * ETag của lần trước, nếu không sẽ tự kéo cả trang JSON mỗi 15 giây suốt ngày trên 4G.
     */
    @GetMapping("/sync")
    public ResponseEntity<OrderSyncResponseDto> sync(
            @RequestParam(required = false) String since,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        OrderSyncResponseDto body = orderSyncService.sync(parseSince(since));
        String etag = orderSyncService.etagFor(body);

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(body);
    }

    /**
     * Chi tiết một đơn kèm dòng món. {@code /sync} cố ý chỉ trả bản tóm tắt (không có
     * {@code items}) để giữ endpoint poll rẻ — màn chi tiết của merchant_app gọi route này
     * khi chủ quán mở một đơn cụ thể, tức là vài lần mỗi đơn chứ không phải mỗi 15 giây.
     */
    @GetMapping("/{id}")
    public OrderResponseDto detail(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }

    @PostMapping("/{id}/transition")
    public OrderResponseDto transition(@PathVariable UUID id, @Valid @RequestBody TransitionOrderStatusRequest request,
            Authentication authentication) {
        OrderStatus to = parseStatus(request.to());
        UUID merchantId = UUID.fromString(authentication.getName());
        return orderService.transitionStatus(id, to, OrderEvent.ActorType.MERCHANT, merchantId, request.reason());
    }

    /**
     * Nút "Đã nhận tiền". BẮT BUỘC {@code Idempotency-Key}, khác với
     * {@code /transition} ở ngay trên — không phải vì kỹ thuật khác nhau mà vì hậu quả khác
     * nhau: đây là route đụng tiền, và merchant_app gửi mọi hành động qua offline queue có
     * retry (bất biến #3, docs/11-merchant-app.md). Một lần retry trên mạng 3G chập chờn
     * không được phép biến thành hai lần ghi nhận thu tiền.
     */
    @PostMapping("/{id}/payment")
    public ResponseEntity<OrderResponseDto> markPayment(@PathVariable UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody MarkPaymentRequest request,
            Authentication authentication) {
        PaymentStatus to = parsePaymentStatus(request.to());
        UUID merchantId = UUID.fromString(authentication.getName());
        // Khoá idempotency tính theo (key, tenant) và hash của request — id đơn nằm trong
        // hash qua bản ghi dưới đây, nên cùng key dùng lại cho ĐƠN KHÁC sẽ ra 409 chứ không
        // âm thầm trả về đơn cũ.
        return idempotencyService.execute(idempotencyKey, new PaymentIdempotencyPayload(id, request),
                OrderResponseDto.class,
                () -> ResponseEntity.ok(orderService.transitionPaymentStatus(id, to,
                        OrderEvent.ActorType.MERCHANT, merchantId, request.reason())));
    }

    /** Chỉ để đưa orderId vào hash idempotency cùng với body — không lộ ra API. */
    private record PaymentIdempotencyPayload(UUID orderId, MarkPaymentRequest request) {
    }

    private Instant parseSince(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidSinceException(raw);
        }
    }

    private OrderStatus parseStatus(String raw) {
        try {
            return OrderStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusValueException(raw);
        }
    }

    private PaymentStatus parsePaymentStatus(String raw) {
        try {
            return PaymentStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidPaymentStatusValueException(raw);
        }
    }

    public static class InvalidStatusValueException extends ApiException {
        public InvalidStatusValueException(String raw) {
            super(HttpStatus.BAD_REQUEST, "INVALID_STATUS_VALUE", "order_status không hợp lệ: " + raw);
        }
    }

    public static class InvalidPaymentStatusValueException extends ApiException {
        public InvalidPaymentStatusValueException(String raw) {
            super(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_STATUS_VALUE", "payment_status không hợp lệ: " + raw);
        }
    }

    public static class InvalidSinceException extends ApiException {
        public InvalidSinceException(String raw) {
            super(HttpStatus.BAD_REQUEST, "INVALID_SINCE",
                    "since phải là mốc thời gian ISO-8601 UTC (vd 2026-08-22T10:15:00Z), nhận được: " + raw);
        }
    }
}
