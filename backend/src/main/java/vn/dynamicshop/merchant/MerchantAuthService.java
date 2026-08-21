package vn.dynamicshop.merchant;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.dynamicshop.common.error.ApiException;
import vn.dynamicshop.common.security.JwtService;
import vn.dynamicshop.common.tenant.TenantContext;
import vn.dynamicshop.merchant.dto.MerchantLoginRequest;
import vn.dynamicshop.merchant.dto.MerchantLoginResponse;

@Service
public class MerchantAuthService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public MerchantAuthService(MerchantRepository merchantRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public MerchantLoginResponse login(MerchantLoginRequest request) {
        Merchant merchant = merchantRepository.findByPhone(request.phone())
                .filter(m -> passwordEncoder.matches(request.password(), m.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException());

        String token = jwtService.issue(Map.of(
                "sub", merchant.getId().toString(),
                "tenant_id", TenantContext.require().toString(),
                "role", merchant.getRole().name()));

        return new MerchantLoginResponse(token, merchant.getId().toString(), merchant.getName(),
                merchant.getRole().name());
    }

    public static class InvalidCredentialsException extends ApiException {
        public InvalidCredentialsException() {
            super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Số điện thoại hoặc mật khẩu không đúng");
        }
    }
}
