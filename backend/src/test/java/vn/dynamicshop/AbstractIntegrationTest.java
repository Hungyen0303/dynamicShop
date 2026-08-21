package vn.dynamicshop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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
 * 🔴 SỬA SAU REVIEW: bản trước dùng role mặc định của Testcontainers ("test") cho CẢ BA
 * việc migrate/app_user/app_admin. Role đó là SUPERUSER — Postgres cho superuser bypass
 * RLS VÔ ĐIỀU KIỆN, kể cả có FORCE ROW LEVEL SECURITY (FORCE chỉ có tác dụng với owner
 * thường, không có tác dụng với superuser/BYPASSRLS). Vì mọi entity dùng Hibernate
 * {@code @TenantId} đã tự filter ở tầng ORM, ba test cross-tenant/GUC trước đó XANH chỉ
 * nhờ tầng Hibernate — RLS (tầng "người từ chối CUỐI CÙNG" theo
 * .claude/skills/tenant-isolation/SKILL.md) chưa từng thực sự được test. Xoá sạch mọi
 * CREATE POLICY khỏi V1 vẫn xanh y hệt — đúng loại lỗi "test xanh trong khi production
 * rò rỉ" mà docs/50-qa.md cảnh báo, chỉ khác là xảy ra ngay cả với Postgres thật.
 *
 * Sửa: tạo thêm hai role KHÔNG superuser/KHÔNG bypassrls bên trong container, mirror đúng
 * app_user/app_admin của infra/docker/postgres/init.sql (role thường + GRANT USAGE +
 * SELECT/INSERT/UPDATE/DELETE, không CREATE). Role superuser mặc định của container chỉ
 * còn đóng vai trò của "postgres" — chạy Flyway migrate, không phải DataSource của app.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static final String APP_USER = "app_user";
    static final String APP_USER_PASSWORD = "app_user_test_pw";
    static final String APP_ADMIN = "app_admin";
    static final String APP_ADMIN_PASSWORD = "app_admin_test_pw";

    static {
        POSTGRES.start();
        bootstrapNonSuperuserRoles();
    }

    /**
     * Chạy TRƯỚC khi Spring context (và do đó Flyway) khởi động, dùng thẳng role superuser
     * mặc định của container ({@link PostgreSQLContainer#getUsername()}) — chỉ để tạo role
     * và cấp quyền, KHÔNG dùng role này cho DataSource nào của app trong test nữa.
     *
     * {@code ALTER DEFAULT PRIVILEGES} phải chạy TRƯỚC khi Flyway tạo bảng — nó chỉ áp dụng
     * cho object được tạo SAU NÀY bởi đúng role đang chạy câu lệnh (ở đây là role superuser
     * của container, cũng chính là role Flyway sẽ dùng để migrate) — y hệt cách
     * infra/docker/postgres/init.sql làm cho môi trường thật.
     */
    private static void bootstrapNonSuperuserRoles() {
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword()); Statement statement = connection.createStatement()) {
            statement.execute("CREATE ROLE %s LOGIN PASSWORD '%s'".formatted(APP_USER, APP_USER_PASSWORD));
            statement.execute("CREATE ROLE %s LOGIN PASSWORD '%s' BYPASSRLS"
                    .formatted(APP_ADMIN, APP_ADMIN_PASSWORD));
            statement.execute("GRANT CONNECT ON DATABASE %s TO %s, %s"
                    .formatted(POSTGRES.getDatabaseName(), APP_USER, APP_ADMIN));
            statement.execute("GRANT USAGE ON SCHEMA public TO %s, %s".formatted(APP_USER, APP_ADMIN));
            statement.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public "
                    + "GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %s, %s".formatted(APP_USER, APP_ADMIN));
            statement.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public "
                    + "GRANT USAGE, SELECT ON SEQUENCES TO %s, %s".formatted(APP_USER, APP_ADMIN));
        } catch (SQLException e) {
            throw new IllegalStateException("Không tạo được role app_user/app_admin trong Testcontainers Postgres",
                    e);
        }
    }

    @DynamicPropertySource
    static void tenantDataSourceProperties(DynamicPropertyRegistry registry) {
        // DataSource chính của app trong test — role app_user THƯỜNG, CHỊU RLS. Đây là
        // datasource mà repository/Hibernate của mọi test dùng — phải KHÔNG bypass RLS,
        // nếu không test cô lập tenant chỉ chứng minh được tầng Hibernate, không phải RLS.
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> APP_USER);
        registry.add("spring.datasource.password", () -> APP_USER_PASSWORD);
        // Một connection duy nhất trong pool ép request sau phải MƯỢN LẠI đúng connection
        // vừa dùng — cách duy nhất khiến test rò rỉ GUC qua pool đáng tin cậy, không may rủi.
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "1");

        // Flyway migrate bằng role superuser của container (đóng vai "postgres") — mirror
        // đúng thiết kế thật: app_user không có quyền CREATE nên không tự migrate được.
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);

        // DataSource admin — role app_admin, BYPASSRLS thật (khác app_user), khớp thiết kế
        // "Admin bypass" trong docs/31-database.md.
        registry.add("app.admin-datasource.url", POSTGRES::getJdbcUrl);
        registry.add("app.admin-datasource.username", () -> APP_ADMIN);
        registry.add("app.admin-datasource.password", () -> APP_ADMIN_PASSWORD);
    }
}
