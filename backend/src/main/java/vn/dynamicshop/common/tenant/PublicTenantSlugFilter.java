package vn.dynamicshop.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Public plane — {@code /v1/s/{slug}/...}. Resolve tenant từ slug trong path (bất biến #1:
 * KHÔNG BAO GIỜ từ @RequestParam/body), set {@link TenantContext}. Route khác (merchant/
 * admin) bỏ qua, tenant của chúng đến từ {@code JwtAuthenticationFilter}.
 *
 * LUÔN clear() trong finally — Tomcat tái sử dụng worker thread, quên dọn sẽ rò rỉ tenant
 * qua thread pool cho request tiếp theo trên cùng thread (khác bản chất nhưng cùng mức độ
 * nghiêm trọng với rò rỉ GUC qua connection pool).
 */
@Component
public class PublicTenantSlugFilter extends OncePerRequestFilter {

    private static final Pattern PUBLIC_PATH = Pattern.compile("^/v1/s/([^/]+)(?:/.*)?$");

    private final TenantRepository tenantRepository;

    public PublicTenantSlugFilter(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Matcher matcher = PUBLIC_PATH.matcher(request.getRequestURI());
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
