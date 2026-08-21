package vn.dynamicshop.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * DataSource CHÍNH của app — role {@code app_user}, CHỊU Row Level Security.
 * Dùng cho public plane (/v1/s/**) và merchant plane (/v1/merchant/**).
 *
 * Khai báo bean tường minh (thay vì để Boot tự autoconfigure) để có thể tồn tại
 * SONG SONG với {@link vn.dynamicshop.admin.AdminDataSourceConfig} (role app_admin,
 * BYPASSRLS) mà không mơ hồ bean nào là "DataSource" mặc định — @Primary quyết định
 * đó luôn là app_user. Không ai vô tình @Autowired nhầm sang admin DataSource vì nó
 * không phải @Primary và phải xin bằng @Qualifier.
 *
 * QUAN TRỌNG: phải bind qua {@link DataSourceProperties} rồi gọi
 * {@code initializeDataSourceBuilder()} — đây là bước Boot tự alias "url" sang
 * "jdbcUrl" của Hikari. Bind thẳng {@code @ConfigurationProperties(prefix="spring.datasource")}
 * lên một {@code DataSourceBuilder.create().build()} KHÔNG hoạt động: HikariDataSource
 * không có setter tên {@code setUrl}, chỉ có {@code setJdbcUrl} — property "url" sẽ bị bỏ
 * qua trong im lặng và jdbcUrl trống (lỗi rất khó nhận ra: Hibernate báo "unable to
 * determine Dialect" thay vì báo thẳng "thiếu url").
 */
@Configuration
public class PrimaryDataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }
}
