package vn.dynamicshop.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Hai mặt phẳng (docs/00-context.md, docs/30-backend.md):
 *   /v1/s/**        public  — không auth, tenant từ slug
 *   /v1/merchant/**auth/login public (đăng nhập chưa có JWT) — còn lại cần Bearer JWT
 *   /v1/admin/**    cần Bearer JWT — Stage 0 chưa có controller nào ở đây
 *
 * Stage 0: JWT tự cấp, không OTP không Zalo (docs/70-stages.md). CSRF tắt vì API thuần
 * JSON dùng Bearer token, không cookie session.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
            JsonAuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/s/**").permitAll()
                        .requestMatchers("/v1/merchant/*/auth/login").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                // 401 chứ không phải 403 mặc định của Spring — xem JsonAuthenticationEntryPoint,
                // đây là điều kiện để luật "gặp 401 thì đăng nhập lại" của merchant_app dùng được.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
