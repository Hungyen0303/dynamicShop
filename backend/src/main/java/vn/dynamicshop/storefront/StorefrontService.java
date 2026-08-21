package vn.dynamicshop.storefront;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.dynamicshop.catalog.CategoryRepository;
import vn.dynamicshop.catalog.ProductRepository;
import vn.dynamicshop.common.tenant.TenantContext;
import vn.dynamicshop.storefront.dto.PublicCategoryDto;
import vn.dynamicshop.storefront.dto.PublicProductDto;
import vn.dynamicshop.storefront.dto.StorefrontResponseDto;
import vn.dynamicshop.common.error.ApiException;

/**
 * Ghép {@code storefronts.layout} + {@code storefronts.theme} thành MỘT object khớp
 * contracts/storefront.schema.json, và resolve mọi {@code data_ref} khai báo trong layout
 * thành dữ liệu thật — client chỉ cần một round-trip (docs/10-customer-app.md bất biến #6).
 */
@Service
public class StorefrontService {

    private static final String CATEGORIES_REF = "categories";
    private static final String PRODUCTS_PREFIX = "products:";

    private final StorefrontRepository storefrontRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public StorefrontService(StorefrontRepository storefrontRepository, CategoryRepository categoryRepository,
            ProductRepository productRepository) {
        this.storefrontRepository = storefrontRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public StorefrontResponseDto getStorefront() {
        UUID tenantId = TenantContext.require();
        Storefront storefront = storefrontRepository.findById(tenantId)
                .orElseThrow(() -> new StorefrontNotConfiguredException(tenantId));

        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("schema_version", storefront.getSchemaVersion());
        layout.put("theme", storefront.getTheme());
        layout.put("screens", storefront.getLayout().get("screens"));

        Map<String, Object> data = resolveData(collectDataRefs(storefront.getLayout()));
        return new StorefrontResponseDto(layout, data);
    }

    /** Duyệt screens.*.blocks[].data_ref, gom lại danh sách khoá cần resolve — bỏ qua data_ref=false. */
    @SuppressWarnings("unchecked")
    private Set<String> collectDataRefs(Map<String, Object> layout) {
        Set<String> refs = new LinkedHashSet<>();
        Object screensObj = layout.get("screens");
        if (!(screensObj instanceof Map<?, ?> screens)) {
            return refs;
        }
        for (Object screen : screens.values()) {
            if (!(screen instanceof Map<?, ?> screenMap)) {
                continue;
            }
            Object blocksObj = screenMap.get("blocks");
            if (!(blocksObj instanceof List<?> blocks)) {
                continue;
            }
            for (Object blockObj : blocks) {
                if (blockObj instanceof Map<?, ?> block && block.get("data_ref") instanceof String ref) {
                    refs.add(ref);
                }
            }
        }
        return refs;
    }

    private Map<String, Object> resolveData(Set<String> refs) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (String ref : refs) {
            if (CATEGORIES_REF.equals(ref)) {
                data.put(ref, categories());
            } else if (ref.startsWith(PRODUCTS_PREFIX)) {
                data.put(ref, productsOfCategory(ref.substring(PRODUCTS_PREFIX.length())));
            }
            // ref lạ không khớp pattern nào — bỏ qua, không throw, để một block cấu hình
            // sai không làm sập cả storefront (đồng bộ tinh thần "block lạ không crash").
        }
        return data;
    }

    private List<PublicCategoryDto> categories() {
        return categoryRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
                .map(PublicCategoryDto::from)
                .toList();
    }

    private List<PublicProductDto> productsOfCategory(String categoryIdRaw) {
        UUID categoryId;
        try {
            categoryId = UUID.fromString(categoryIdRaw);
        } catch (IllegalArgumentException e) {
            return new ArrayList<>();
        }
        return productRepository.findByCategory_IdAndDeletedAtIsNull(categoryId).stream()
                .map(PublicProductDto::from)
                .toList();
    }

    public static class StorefrontNotConfiguredException extends ApiException {
        public StorefrontNotConfiguredException(UUID tenantId) {
            super(HttpStatus.NOT_FOUND, "STOREFRONT_NOT_CONFIGURED", "Tenant %s chưa có storefront".formatted(tenantId));
        }
    }
}
