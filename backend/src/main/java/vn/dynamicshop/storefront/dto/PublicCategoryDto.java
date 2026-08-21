package vn.dynamicshop.storefront.dto;

import java.util.UUID;
import vn.dynamicshop.catalog.Category;

/** DTO riêng cho public plane — bất biến #8, không serialize entity {@link Category}. */
public record PublicCategoryDto(UUID id, String name, int sortOrder) {

    public static PublicCategoryDto from(Category category) {
        return new PublicCategoryDto(category.getId(), category.getName(), category.getSortOrder());
    }
}
