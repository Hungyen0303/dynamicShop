package vn.dynamicshop.merchant.dto;

import jakarta.validation.constraints.NotBlank;

public record MerchantLoginRequest(@NotBlank String phone, @NotBlank String password) {
}
