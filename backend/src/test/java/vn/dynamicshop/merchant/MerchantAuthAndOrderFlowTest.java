package vn.dynamicshop.merchant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import vn.dynamicshop.order.OrderRepository;

/**
 * Bước 6 của flow đầu-cuối Stage 0 (docs/70-stages.md): "Gọi API xác nhận đơn → trạng
 * thái đổi, order_events thêm dòng". Chạm cả hai mặt phẳng: login lấy JWT (tenant từ
 * slug), rồi dùng JWT đó gọi endpoint merchant (tenant từ JWT claim).
 */
@AutoConfigureMockMvc
class MerchantAuthAndOrderFlowTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void dang_nhap_roi_xac_nhan_don_thanh_cong() throws Exception {
        String slug = "merchant-flow-" + UUID.randomUUID();
        Tenant tenant = tenantRepository.save(new Tenant(slug, "Quán test merchant flow"));

        UUID productId;
        TenantContext.set(tenant.getId());
        try {
            merchantRepository.save(new Merchant("0909999999", passwordEncoder.encode("secret123"), "Chủ quán",
                    Merchant.MerchantRole.OWNER));
            Category category = categoryRepository.save(new Category("Món chính", 1));
            productId = productRepository.save(new Product(category, "Phở gà", 40000, null, true)).getId();
        } finally {
            TenantContext.clear();
        }

        // 1. Đăng nhập — tenant từ slug trong path (bootstrap, xem MerchantLoginTenantSlugFilter)
        String loginBody = """
                {"phone":"0909999999","password":"secret123"}
                """;
        String loginJson = mockMvc.perform(post("/v1/merchant/{slug}/auth/login", slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        var loginResponse = objectMapper.readValue(loginJson, java.util.Map.class);
        String token = (String) loginResponse.get("token");
        assertThat(token).isNotBlank();

        // 2. Khách tạo đơn (public plane)
        String orderBody = "{\"items\":[{\"productId\":\"%s\",\"qty\":1}]}".formatted(productId);
        String orderJson = mockMvc.perform(post("/v1/s/{slug}/orders", slug)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        var orderResponse = objectMapper.readValue(orderJson, java.util.Map.class);
        String orderId = (String) orderResponse.get("id");
        assertThat(orderResponse.get("orderStatus")).isEqualTo("PENDING");

        // 3. Merchant xác nhận đơn — tenant từ JWT claim (authenticated plane)
        mockMvc.perform(post("/v1/merchant/orders/{id}/transition", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"CONFIRMED\",\"reason\":\"đã xác nhận với khách\"}"))
                .andExpect(status().isOk());

        TenantContext.set(tenant.getId());
        try {
            var order = orderRepository.findById(UUID.fromString(orderId)).orElseThrow();
            assertThat(order.getOrderStatus().name()).isEqualTo("CONFIRMED");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void sai_mat_khau_bi_tu_choi() throws Exception {
        String slug = "merchant-badpw-" + UUID.randomUUID();
        Tenant tenant = tenantRepository.save(new Tenant(slug, "Quán test sai mật khẩu"));
        TenantContext.set(tenant.getId());
        try {
            merchantRepository.save(new Merchant("0908888888", passwordEncoder.encode("dungmatkhau"), "Chủ quán",
                    Merchant.MerchantRole.OWNER));
        } finally {
            TenantContext.clear();
        }

        mockMvc.perform(post("/v1/merchant/{slug}/auth/login", slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0908888888\",\"password\":\"saimatkhau\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void goi_endpoint_merchant_khong_co_token_bi_tu_choi() throws Exception {
        // Sprint 2.1b: đã thay Http403ForbiddenEntryPoint mặc định của Spring bằng
        // JsonAuthenticationEntryPoint → 401 kèm body JSON, không còn 403.
        mockMvc.perform(post("/v1/merchant/orders/{id}/transition", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"CONFIRMED\"}"))
                .andExpect(status().isUnauthorized());
    }
}
