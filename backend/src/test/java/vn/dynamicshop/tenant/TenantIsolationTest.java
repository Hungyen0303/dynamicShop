package vn.dynamicshop.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.dynamicshop.AbstractIntegrationTest;
import vn.dynamicshop.catalog.Category;
import vn.dynamicshop.catalog.CategoryRepository;
import vn.dynamicshop.common.tenant.Tenant;
import vn.dynamicshop.common.tenant.TenantContext;
import vn.dynamicshop.common.tenant.TenantRepository;
import vn.dynamicshop.order.Order;
import vn.dynamicshop.order.OrderRepository;

/**
 * 🔴 Test bắt buộc, chặn merge nếu đỏ — docs/50-qa.md mục 1, .claude/skills/tenant-isolation.
 * Dùng Testcontainers Postgres thật (KHÔNG H2 — RLS là tính năng của Postgres).
 */
class TenantIsolationTest extends AbstractIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Tenant createTenant(String slug) {
        // Bảng tenants không có RLS — không cần TenantContext để ghi.
        return tenantRepository.save(new Tenant(slug, slug + " shop"));
    }

    private Order seedOrder(Tenant tenant, String code) {
        TenantContext.set(tenant.getId());
        try {
            return orderRepository.save(new Order(code, null, null, null, null));
        } finally {
            TenantContext.clear();
        }
    }

    private Category seedCategory(Tenant tenant, String name) {
        TenantContext.set(tenant.getId());
        try {
            return categoryRepository.save(new Category(name, 0));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void tenant_b_khong_doc_duoc_du_lieu_cua_a() {
        Tenant a = createTenant("test-a-" + UUID.randomUUID());
        Tenant b = createTenant("test-b-" + UUID.randomUUID());

        Order orderA1 = seedOrder(a, "A-001");
        seedOrder(a, "A-002");
        seedOrder(a, "A-003");
        seedOrder(b, "B-001");
        seedOrder(b, "B-002");

        TenantContext.set(b.getId());
        try {
            List<Order> visible = orderRepository.findAll();
            assertThat(visible).hasSize(2);
            assertThat(visible).allMatch(o -> o.getTenantId().equals(b.getId()));

            // ← dòng đáng giá nhất: findById lộ qua URL/log, không phải chỉ findAll bị filter
            assertThat(orderRepository.findById(orderA1.getId())).isEmpty();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void tenant_b_khong_doc_duoc_categories_cua_a() {
        Tenant a = createTenant("test-a-" + UUID.randomUUID());
        Tenant b = createTenant("test-b-" + UUID.randomUUID());

        Category catA = seedCategory(a, "Bún");
        seedCategory(b, "Trà sữa");
        seedCategory(b, "Bánh ngọt");

        TenantContext.set(b.getId());
        try {
            assertThat(categoryRepository.findAll()).hasSize(2);
            assertThat(categoryRepository.findById(catA.getId())).isEmpty();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void guc_khong_ro_ri_qua_connection_pool() {
        Tenant a = createTenant("test-a-" + UUID.randomUUID());
        seedOrder(a, "POOL-001");

        TenantContext.set(a.getId());
        try {
            orderRepository.findAll();
        } finally {
            TenantContext.clear();
        }

        // mượn lại connection từ pool (maximum-pool-size=1 trong test ép dùng lại đúng
        // connection vừa rồi) — nếu SET_LOCAL bị thay bằng SET, giá trị sẽ còn sót lại đây.
        String guc = jdbcTemplate.queryForObject(
                "SELECT current_setting('app.tenant_id', true)", String.class);
        assertThat(guc).isNullOrEmpty();
    }

    @Test
    void moi_bang_co_tenant_id_deu_bat_rls() {
        List<String> tablesWithTenantId = jdbcTemplate.queryForList(
                "SELECT DISTINCT table_name FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND column_name = 'tenant_id'",
                String.class);

        assertThat(tablesWithTenantId).isNotEmpty();

        for (String table : tablesWithTenantId) {
            Boolean rlsEnabled = jdbcTemplate.queryForObject(
                    "SELECT relrowsecurity FROM pg_class WHERE relname = ? AND relnamespace = 'public'::regnamespace",
                    Boolean.class, table);
            Boolean rlsForced = jdbcTemplate.queryForObject(
                    "SELECT relforcerowsecurity FROM pg_class WHERE relname = ? AND relnamespace = 'public'::regnamespace",
                    Boolean.class, table);
            assertThat(rlsEnabled).as("RLS chưa ENABLE cho bảng %s", table).isTrue();
            assertThat(rlsForced).as("RLS chưa FORCE cho bảng %s", table).isTrue();
        }
    }
}
