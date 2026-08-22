package vn.dynamicshop.notification.dto;

import java.time.Instant;
import java.util.UUID;
import vn.dynamicshop.notification.DeviceToken;

/**
 * DTO riêng, không serialize entity (bất biến #8). Cố ý KHÔNG trả lại {@code token} —
 * client vừa gửi nó lên, không cần nhận lại, và không có lý do gì để nó xuất hiện thêm một
 * lần nữa trong log/response.
 */
public record DeviceTokenResponseDto(
        UUID id,
        String platform,
        String appVersion,
        Instant lastSeenAt) {

    public static DeviceTokenResponseDto from(DeviceToken deviceToken) {
        return new DeviceTokenResponseDto(
                deviceToken.getId(),
                deviceToken.getPlatform().name(),
                deviceToken.getAppVersion(),
                deviceToken.getLastSeenAt());
    }
}
