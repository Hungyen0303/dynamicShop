package vn.dynamicshop.merchant.dto;

public record MerchantLoginResponse(String token, String merchantId, String name, String role) {
}
