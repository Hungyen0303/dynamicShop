package vn.dynamicshop.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.dynamicshop.common.error.ApiException;
import vn.dynamicshop.notification.dto.DeviceTokenResponseDto;
import vn.dynamicshop.notification.dto.RegisterDeviceRequest;

@Service
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    /**
     * Upsert theo {@code token} trong phạm vi tenant hiện tại. merchant_app gọi lại mỗi lần
     * đăng nhập và mỗi lần FCM xoay token, nên đây là đường đi thường xuyên, không phải
     * ngoại lệ — tạo dòng mới mỗi lần sẽ làm outbox worker gửi trùng lên cùng một máy.
     */
    @Transactional
    public DeviceTokenResponseDto register(UUID merchantId, RegisterDeviceRequest request) {
        DeviceToken.Platform platform = parsePlatform(request.platform());
        DeviceToken deviceToken = deviceTokenRepository.findByToken(request.token())
                .map(existing -> {
                    existing.refresh(merchantId, platform, request.appVersion());
                    return existing;
                })
                .orElseGet(() -> deviceTokenRepository.save(
                        new DeviceToken(merchantId, request.token(), platform, request.appVersion())));
        return DeviceTokenResponseDto.from(deviceToken);
    }

    /**
     * Thu hồi khi đăng xuất. Không tìm thấy token thì im lặng bỏ qua — đăng xuất phải luôn
     * thành công ở phía người dùng, và "token này vốn không có ở đây" chính là trạng thái
     * mà lời gọi muốn đạt tới.
     */
    @Transactional
    public void revoke(String token) {
        deviceTokenRepository.findByToken(token).ifPresent(DeviceToken::revoke);
    }

    /**
     * Token còn sống của tenant hiện tại — dùng bởi {@code OutboxBatchProcessor} để fan-out
     * push. Không nhận tenantId làm tham số: tenant đến từ {@code TenantContext} mà worker
     * đã set, đúng bất biến #1.
     */
    @Transactional(readOnly = true)
    public List<String> activeTokensForCurrentTenant() {
        return deviceTokenRepository.findByRevokedAtIsNull().stream().map(DeviceToken::getToken).toList();
    }

    private DeviceToken.Platform parsePlatform(String raw) {
        try {
            return DeviceToken.Platform.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidPlatformException(raw);
        }
    }

    public static class InvalidPlatformException extends ApiException {
        public InvalidPlatformException(String raw) {
            super(HttpStatus.BAD_REQUEST, "INVALID_PLATFORM", "platform không hợp lệ: " + raw);
        }
    }
}
