package vn.dynamicshop.merchant;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.dynamicshop.merchant.dto.MerchantLoginRequest;
import vn.dynamicshop.merchant.dto.MerchantLoginResponse;

/**
 * {@code slug} chỉ dùng để {@link MerchantLoginTenantSlugFilter} resolve tenant trước khi
 * vào tới đây — controller không đọc lại slug, tenant đến từ TenantContext (một nguồn
 * sự thật duy nhất).
 */
@RestController
@RequestMapping("/v1/merchant/{slug}/auth")
public class MerchantAuthController {

    private final MerchantAuthService merchantAuthService;

    public MerchantAuthController(MerchantAuthService merchantAuthService) {
        this.merchantAuthService = merchantAuthService;
    }

    @PostMapping("/login")
    public MerchantLoginResponse login(@PathVariable String slug, @Valid @RequestBody MerchantLoginRequest request) {
        return merchantAuthService.login(request);
    }
}
