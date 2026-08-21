package vn.dynamicshop.storefront.dto;

import java.util.UUID;
import vn.dynamicshop.catalog.Product;

/**
 * DTO riêng cho public plane — bất biến #8. KHÔNG có field như giá vốn; chỉ những gì
 * khách được thấy. Test {@code StorefrontApiTest} kiểm tra JSON response không chứa key
 * cấm (ví dụ "costPrice", "tenantId").
 */
public record PublicProductDto(UUID id, String name, long price, String imageUrl, boolean available) {

    public static PublicProductDto from(Product product) {
        return new PublicProductDto(product.getId(), product.getName(), product.getPrice(),
                product.getImageUrl(), product.isAvailable());
    }
}
