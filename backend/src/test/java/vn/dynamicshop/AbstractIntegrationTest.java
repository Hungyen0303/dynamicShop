package vn.dynamicshop;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base cho MỌI test cần Postgres thật (bắt buộc cho test cô lập tenant — RLS không mock
 * được, không dùng H2). Container static + @DynamicPropertySource là idiom chuẩn của
 * Testcontainers + JUnit5, khởi động một lần, dùng chung cho cả class.
 *
 * KHÔNG dùng {@code @ServiceConnection} — DataSource chính (app_user) và DataSource admin
 * (app_admin) đều khai báo tường minh bằng @ConfigurationProperties (tách vật lý theo
 * docs/31-database.md "Admin bypass"), @ServiceConnection không "thấy" các bean đó.
 *
 * Test dùng CHUNG một role Postgres (superuser mặc định của testcontainers) cho cả ba việc
 * migrate/app_user/app_admin — chấp nhận được vì mọi bảng đều FORCE ROW LEVEL SECURITY,
 * nên RLS áp dụng bất kể ai là owner. Test cô lập tenant vẫn có giá trị dù không dùng đúng
 * role app_user/app_admin như production.
 *
 * "Singleton container" — CỐ Ý không dùng @Testcontainers/@Container: annotation đó dừng
 * container sau MỖI test class, nhưng field static này bị NHIỀU test class kế thừa dùng
 * chung. Nếu để @Container tự quản, class chạy sau sẽ gặp connection refused vì class
 * chạy trước đã dừng container hộ nó. Khởi động thủ công một lần, để Ryuk dọn khi JVM thoát.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void tenantDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Một connection duy nhất trong pool ép request sau phải MƯỢN LẠI đúng connection
        // vừa dùng — cách duy nhất khiến test rò rỉ GUC qua pool đáng tin cậy, không may rủi.
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "1");

        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);

        registry.add("app.admin-datasource.url", POSTGRES::getJdbcUrl);
        registry.add("app.admin-datasource.username", POSTGRES::getUsername);
        registry.add("app.admin-datasource.password", POSTGRES::getPassword);
    }
}
