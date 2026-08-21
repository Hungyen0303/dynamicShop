package vn.dynamicshop.storefront;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.dynamicshop.storefront.dto.StorefrontResponseDto;

/**
 * Public plane — không auth, tenant từ slug (đã resolve bởi {@code PublicTenantSlugFilter}
 * trước khi vào tới đây). {@code slug} chỉ dùng để route tới đúng filter, KHÔNG dùng lại
 * ở tầng service — tenant lấy từ {@code TenantContext}, không phải từ path variable này,
 * để không có hai nguồn sự thật khác nhau (đúng bất biến #1).
 */
@RestController
@RequestMapping("/v1/s/{slug}")
public class StorefrontController {

    private final StorefrontService storefrontService;

    public StorefrontController(StorefrontService storefrontService) {
        this.storefrontService = storefrontService;
    }

    @GetMapping("/storefront")
    public StorefrontResponseDto getStorefront(@PathVariable String slug,
            @RequestParam(defaultValue = "3") int schema) {
        return storefrontService.getStorefront();
    }
}
