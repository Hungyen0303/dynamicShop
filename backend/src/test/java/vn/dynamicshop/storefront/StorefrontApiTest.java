package vn.dynamicshop.storefront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import vn.dynamicshop.AbstractIntegrationTest;
import vn.dynamicshop.catalog.Category;
import vn.dynamicshop.catalog.CategoryRepository;
import vn.dynamicshop.catalog.Product;
import vn.dynamicshop.catalog.ProductRepository;
import vn.dynamicshop.common.tenant.Tenant;
import vn.dynamicshop.common.tenant.TenantContext;
import vn.dynamicshop.common.tenant.TenantRepository;

/**
 * docs/30-backend.md mục "Storefront API — nơi dễ rò rỉ nhất": MỘT response gồm layout +
 * data đã resolve sẵn, và response KHÔNG chứa field cấm (test kiểm tra JSON không lộ
 * tenant_id/cost_price... của entity).
 */
@AutoConfigureMockMvc
class StorefrontApiTest extends AbstractIntegrationTest {

    private static final List<String> FORBIDDEN_KEYS = List.of("tenantId", "tenant_id", "costPrice", "cost_price",
            "passwordHash", "password_hash", "deletedAt", "deleted_at");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private StorefrontRepository storefrontRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void storefront_tra_ve_layout_va_data_trong_mot_response_khong_lo_field_cam() throws Exception {
        String slug = "sf-test-" + UUID.randomUUID();
        Tenant tenant = tenantRepository.save(new Tenant(slug, "Quán test storefront"));

        TenantContext.set(tenant.getId());
        try {
            Category category = categoryRepository.save(new Category("Đồ uống", 1));
            productRepository.save(new Product(category, "Trà đá", 5000, null, true));

            storefrontRepository.save(new Storefront(tenant.getId(), 3,
                    Map.of("screens", Map.of("home", Map.of("blocks", List.of(
                            Map.of("id", "cats_1", "type", "category_row", "v", 1,
                                    "props", Map.of(), "data_ref", "categories"),
                            Map.of("id", "grid_1", "type", "product_grid", "v", 1,
                                    "props", Map.of(), "data_ref", "products:" + category.getId()))))),
                    Map.of("primary", "#E23744", "radius_md", 12, "font_family", "Be Vietnam Pro")));
        } finally {
            TenantContext.clear();
        }

        String json = mockMvc.perform(get("/v1/s/{slug}/storefront", slug).param("schema", "3"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertNoForbiddenKeys(json);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(json, Map.class);
        assertThat(body).containsKeys("layout", "data");
        @SuppressWarnings("unchecked")
        Map<String, Object> layout = (Map<String, Object>) body.get("layout");
        assertThat(layout.get("schema_version")).isEqualTo(3);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).containsKeys("categories");
    }

    @Test
    void hai_tenant_co_theme_khac_han() throws Exception {
        String slugA = "theme-a-" + UUID.randomUUID();
        String slugB = "theme-b-" + UUID.randomUUID();
        Tenant a = tenantRepository.save(new Tenant(slugA, "Shop A"));
        Tenant b = tenantRepository.save(new Tenant(slugB, "Shop B"));

        seedEmptyStorefront(a, "#E23744");
        seedEmptyStorefront(b, "#0EA5A4");

        String jsonA = mockMvc.perform(get("/v1/s/{slug}/storefront", slugA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String jsonB = mockMvc.perform(get("/v1/s/{slug}/storefront", slugB))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(jsonA).contains("#E23744").doesNotContain("#0EA5A4");
        assertThat(jsonB).contains("#0EA5A4").doesNotContain("#E23744");
    }

    private void seedEmptyStorefront(Tenant tenant, String primary) {
        TenantContext.set(tenant.getId());
        try {
            storefrontRepository.save(new Storefront(tenant.getId(), 3,
                    Map.of("screens", Map.of("home", Map.of("blocks", List.of()))),
                    Map.of("primary", primary, "radius_md", 12, "font_family", "Be Vietnam Pro")));
        } finally {
            TenantContext.clear();
        }
    }

    private void assertNoForbiddenKeys(String json) {
        for (String key : FORBIDDEN_KEYS) {
            assertThat(json).as("response chứa field cấm '%s'", key).doesNotContain("\"" + key + "\"");
        }
    }
}
