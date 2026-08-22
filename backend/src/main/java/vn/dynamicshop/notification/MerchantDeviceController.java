package vn.dynamicshop.notification;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.dynamicshop.notification.dto.DeviceTokenResponseDto;
import vn.dynamicshop.notification.dto.RegisterDeviceRequest;
import vn.dynamicshop.notification.dto.RevokeDeviceRequest;

/**
 * Authenticated plane — tenant từ JWT claim. merchant_app gọi {@code POST} sau khi đăng
 * nhập và mỗi lần FCM xoay token, gọi {@code DELETE} khi đăng xuất.
 *
 * Cả hai route nhận token trong BODY, kể cả {@code DELETE} (hợp lệ với Spring MVC) — token
 * đặt trong query param sẽ nằm lại trong access log của Caddy, trong lịch sử shell lúc
 * debug bằng curl, và trong mọi proxy trên đường đi. Nó là thứ đủ để gửi thông báo giả
 * mạo tới máy chủ quán, nên không để nó lọt vào URL.
 *
 * Không yêu cầu {@code Idempotency-Key}: hai route đều idempotent tự nhiên theo thiết kế
 * (upsert theo token, revoke bỏ qua khi không tìm thấy), gọi lại bao nhiêu lần cũng cho ra
 * cùng một trạng thái và không tạo thực thể thứ hai.
 */
@RestController
@RequestMapping("/v1/merchant/devices")
public class MerchantDeviceController {

    private final DeviceTokenService deviceTokenService;

    public MerchantDeviceController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @PostMapping
    public DeviceTokenResponseDto register(@Valid @RequestBody RegisterDeviceRequest request,
            Authentication authentication) {
        UUID merchantId = UUID.fromString(authentication.getName());
        return deviceTokenService.register(merchantId, request);
    }

    @DeleteMapping
    public ResponseEntity<Void> revoke(@Valid @RequestBody RevokeDeviceRequest request) {
        deviceTokenService.revoke(request.token());
        return ResponseEntity.noContent().build();
    }
}
