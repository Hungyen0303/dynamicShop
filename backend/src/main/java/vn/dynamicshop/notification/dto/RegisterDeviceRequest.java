package vn.dynamicshop.notification.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code token} là chuỗi opaque do SDK FCM sinh ra trên máy — backend không parse, không
 * validate định dạng (Google có thể đổi bất cứ lúc nào), chỉ lưu nguyên văn.
 */
public record RegisterDeviceRequest(
        @NotBlank String token,
        @NotBlank String platform,
        String appVersion) {
}
