package vn.dynamicshop.notification.dto;

import jakarta.validation.constraints.NotBlank;

/** Token đi trong body chứ không phải query param — xem ghi chú ở {@code MerchantDeviceController}. */
public record RevokeDeviceRequest(@NotBlank String token) {
}
