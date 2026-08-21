package vn.dynamicshop.common.tenant;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Đẩy advisor của @Transactional ra NGOÀI CÙNG (HIGHEST_PRECEDENCE) trong chuỗi proxy AOP,
 * để {@link TenantGucAdvisorConfig} (order mặc định, LOWEST_PRECEDENCE) luôn chạy SAU khi
 * transaction đã bắt đầu. Nhờ vậy lệnh SET LOCAL trong {@link TenantGucInterceptor} luôn
 * nằm trong đúng transaction, không phải một statement auto-commit rời rạc.
 *
 * Khai báo @EnableTransactionManagement ở đây khiến Boot tự lùi bước autoconfiguration
 * mặc định của nó (TransactionAutoConfiguration có @ConditionalOnMissingBean cho chính
 * cấu hình loại này) — không có xung đột bean.
 */
@Configuration
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
public class TransactionOrderConfig {
}
