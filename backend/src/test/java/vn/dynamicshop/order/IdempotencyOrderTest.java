package vn.dynamicshop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vn.dynamicshop.AbstractIntegrationTest;
import vn.dynamicshop.catalog.Category;
import vn.dynamicshop.catalog.CategoryRepository;
import vn.dynamicshop.catalog.Product;
import vn.dynamicshop.catalog.ProductRepository;
import vn.dynamicshop.common.tenant.Tenant;
import vn.dynamicshop.common.tenant.TenantContext;
import vn.dynamicshop.common.tenant.TenantRepository;

/**
 * docs/70-stages.md bước 7 của flow đầu-cuối: "Gửi lại đúng request đặt hàng đó → KHÔNG
 * tạo đơn thứ hai". Test qua HTTP thật (MockMvc) — chạm cả filter resolve tenant từ slug,
 * idempotency, và tạo đơn, đúng như luồng thật.
 */
@AutoConfigureMockMvc
class IdempotencyOrderTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    @Test
    void gui_hai_lan_cung_idempotency_key_chi_tao_mot_don() throws Exception {
        String slug = "idem-test-" + UUID.randomUUID();
        Tenant tenant = tenantRepository.save(new Tenant(slug, "Quán test idempotency"));

        UUID productId;
        TenantContext.set(tenant.getId());
        try {
            Category category = categoryRepository.save(new Category("Món chính", 1));
            Product product = productRepository.save(new Product(category, "Phở bò", 45000, null, true));
            productId = product.getId();
        } finally {
            TenantContext.clear();
        }

        String idempotencyKey = UUID.randomUUID().toString();
        String body = """
                {"items":[{"productId":"%s","qty":2}],"note":"không hành","phone":"0900000000"}
                """.formatted(productId);

        mockMvc.perform(post("/v1/s/{slug}/orders", slug)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/s/{slug}/orders", slug)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        TenantContext.set(tenant.getId());
        try {
            assertThat(orderRepository.findAll()).hasSize(1);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void thieu_idempotency_key_tra_ve_400() throws Exception {
        String slug = "idem-missing-" + UUID.randomUUID();
        tenantRepository.save(new Tenant(slug, "Quán test thiếu key"));

        mockMvc.perform(post("/v1/s/{slug}/orders", slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cung_key_khac_noi_dung_tra_ve_409() throws Exception {
        String slug = "idem-conflict-" + UUID.randomUUID();
        Tenant tenant = tenantRepository.save(new Tenant(slug, "Quán test conflict"));

        UUID productId;
        TenantContext.set(tenant.getId());
        try {
            Category category = categoryRepository.save(new Category("Món chính", 1));
            productId = productRepository.save(new Product(category, "Bún chả", 40000, null, true)).getId();
        } finally {
            TenantContext.clear();
        }

        String key = UUID.randomUUID().toString();
        mockMvc.perform(post("/v1/s/{slug}/orders", slug)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":\"%s\",\"qty\":1}]}".formatted(productId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/s/{slug}/orders", slug)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":\"%s\",\"qty\":5}]}".formatted(productId)))
                .andExpect(status().isConflict());
    }
}
