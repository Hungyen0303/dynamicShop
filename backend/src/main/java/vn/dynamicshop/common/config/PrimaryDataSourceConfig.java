package vn.dynamicshop.common.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

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

    /**
     * 🔴 Sửa sau review: khai báo tường minh, ĐỪNG xoá — {@code AdminDataSourceConfig} định
     * nghĩa bean {@code adminJdbcTemplate} kiểu {@code JdbcTemplate}. Autoconfigure mặc định
     * của Boot cho {@code JdbcTemplate} có {@code @ConditionalOnMissingBean(JdbcTemplate.class)}
     * — check THEO TYPE, không theo tên — nên hễ {@code adminJdbcTemplate} tồn tại, Boot sẽ
     * KHÔNG tự tạo bean "jdbcTemplate" mặc định cho DataSource chính nữa. Hậu quả: bất kỳ chỗ
     * nào {@code @Autowired JdbcTemplate} không kèm @Qualifier sẽ vô tình lấy NHẦM bean admin
     * (BYPASSRLS) — đúng bug đã bắt được lúc review (test cô lập tenant lấy nhầm JdbcTemplate
     * admin, RLS bị bỏ qua trong im lặng). Khai báo @Primary tường minh ở đây để không bao giờ
     * mơ hồ, giống triết lý tách vật lý DataSource phía trên.
     */
    @Primary
    @Bean(name = "jdbcTemplate")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
