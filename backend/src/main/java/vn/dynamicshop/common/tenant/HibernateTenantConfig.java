package vn.dynamicshop.common.tenant;

import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Đăng ký {@link TenantIdentifierResolver} với Hibernate để entity đánh dấu
 * {@code @org.hibernate.annotations.TenantId} tự động lọc theo tenant hiện tại
 * (partitioned multi-tenancy — cùng một DataSource, không tách schema/DB).
 */
@Configuration
public class HibernateTenantConfig {

    @Bean
    public HibernatePropertiesCustomizer tenantIdentifierResolverCustomizer(
            TenantIdentifierResolver resolver) {
        return properties -> properties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
