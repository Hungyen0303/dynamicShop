package vn.dynamicshop.storefront.dto;

import java.util.Map;

/**
 * {@code GET /v1/s/{slug}/storefront?schema=3} — MỘT response, server resolve sẵn mọi
 * {@code data_ref} (docs/30-backend.md mục "Storefront API").
 */
public record StorefrontResponseDto(Map<String, Object> layout, Map<String, Object> data) {
}
