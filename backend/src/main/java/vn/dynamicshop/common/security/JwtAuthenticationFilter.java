package vn.dynamicshop.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import vn.dynamicshop.common.tenant.TenantContext;

/**
 * Authenticated plane — {@code /v1/merchant/**}, {@code /v1/admin/**}. Tenant lấy từ JWT
 * claim {@code tenant_id} (bất biến #1), KHÔNG BAO GIỜ từ request. Đặt
 * {@link org.springframework.security.core.context.SecurityContextHolder} (ai đang gọi)
 * và {@link TenantContext} (tenant nào) cùng lúc từ cùng một token đã verify chữ ký.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        try {
            if (header != null && header.startsWith("Bearer ")) {
                Map<String, Object> claims;
                try {
                    claims = jwtService.verify(header.substring("Bearer ".length()));
                } catch (JwtService.InvalidJwtException e) {
                    // JSON cùng shape với phần còn lại của API, không phải trang lỗi HTML của
                    // container (sendError cũ). merchant_app parse được để biết chắc phải đăng
                    // nhập lại — xem JsonAuthenticationEntryPoint.
                    JsonAuthenticationEntryPoint.writeUnauthenticated(objectMapper, response, e.getMessage());
                    return;
                }
                String subject = String.valueOf(claims.get("sub"));
                String role = String.valueOf(claims.getOrDefault("role", "MERCHANT"));
                Object tenantIdClaim = claims.get("tenant_id");
                if (tenantIdClaim != null) {
                    TenantContext.set(UUID.fromString(String.valueOf(tenantIdClaim)));
                }
                var authentication = new UsernamePasswordAuthenticationToken(
                        subject, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
