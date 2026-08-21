package vn.dynamicshop.common.tenant;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

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

    /**
     * Thay bean {@code transactionManager} Boot tự autoconfigure (tên bean phải khớp để
     * {@code @ConditionalOnMissingBean} của Boot lùi bước) — xem lý do ở
     * {@link TenantAwareJpaTransactionManager}. Đây là DataSource chính (app_user) duy nhất
     * có JPA/EntityManagerFactory trong app; admin/ chỉ có JdbcTemplate, không cần tx manager
     * riêng.
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new TenantAwareJpaTransactionManager(entityManagerFactory);
    }
}
