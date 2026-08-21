package vn.dynamicshop.admin;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DataSource thứ hai — role {@code app_admin}, BYPASSRLS.
 *
 * Tách VẬT LÝ khỏi {@link vn.dynamicshop.common.config.PrimaryDataSourceConfig} và
 * chỉ khai báo trong package {@code admin/} — đúng tinh thần AGENTS.md bất biến #2
 * và docs/31-database.md mục "Admin bypass": "Đừng viết exception vào policy. Dùng
 * DataSource thứ hai... Tách vật lý thì không thể dùng nhầm."
 *
 * Không @Primary — mọi nơi muốn dùng datasource này phải @Qualifier("adminDataSource")
 * tường minh, và theo quy ước chỉ code trong package admin/ mới được làm vậy.
 */
@Configuration
public class AdminDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.admin-datasource")
    public DataSourceProperties adminDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "adminDataSource")
    @ConfigurationProperties(prefix = "app.admin-datasource.hikari")
    public HikariDataSource adminDataSource(
            @Qualifier("adminDataSourceProperties") DataSourceProperties adminDataSourceProperties) {
        return adminDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "adminJdbcTemplate")
    public JdbcTemplate adminJdbcTemplate(@Qualifier("adminDataSource") DataSource adminDataSource) {
        return new JdbcTemplate(adminDataSource);
    }
}
