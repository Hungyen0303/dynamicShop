package vn.dynamicshop.merchant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.dynamicshop.common.tenant.Tenant;
import vn.dynamicshop.common.tenant.TenantContext;
import vn.dynamicshop.common.tenant.TenantNotFoundException;
import vn.dynamicshop.common.tenant.TenantRepository;

/**
 * Đăng nhập merchant là điểm gà-và-trứng: chưa có JWT nên chưa biết tenant, nhưng cần
 * tenant để tra {@code merchants} (bảng có RLS). Giải pháp: dùng CHÍNH cơ chế đã được
 * duyệt cho public plane — slug trong path — chỉ cho route bootstrap này
 * ({@code POST /v1/merchant/{slug}/auth/login}). Mọi route merchant khác sau khi đăng
 * nhập vẫn resolve tenant từ JWT claim như kiến trúc quy định.
 */
@Component
public class MerchantLoginTenantSlugFilter extends OncePerRequestFilter {

    private static final Pattern LOGIN_PATH = Pattern.compile("^/v1/merchant/([^/]+)/auth/login$");

    private final TenantRepository tenantRepository;

    public MerchantLoginTenantSlugFilter(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Matcher matcher = LOGIN_PATH.matcher(request.getRequestURI());
        if (!matcher.matches()) {
            chain.doFilter(request, response);
            return;
        }
        String slug = matcher.group(1);
        try {
            Tenant tenant = tenantRepository.findBySlug(slug).orElseThrow(() -> new TenantNotFoundException(slug));
            TenantContext.set(tenant.getId());
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
