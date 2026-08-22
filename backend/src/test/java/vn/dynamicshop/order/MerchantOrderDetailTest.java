package vn.dynamicshop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import vn.dynamicshop.AbstractIntegrationTest;
import vn.dynamicshop.catalog.Category;
import vn.dynamicshop.catalog.CategoryRepository;
import vn.dynamicshop.catalog.Product;
import vn.dynamicshop.catalog.ProductRepository;
import vn.dynamicshop.common.tenant.Tenant;
import vn.dynamicshop.common.tenant.TenantContext;
import vn.dynamicshop.common.tenant.TenantRepository;
import vn.dynamicshop.merchant.Merchant;
import vn.dynamicshop.merchant.MerchantRepository;

/**
 * {@code GET /v1/merchant/orders/{id}} — sprint 2.1b.
 *
 * Vì sao cần route riêng: {@code /sync} cố ý chỉ trả bản tóm tắt (không có {@code items})
 * để giữ endpoint poll rẻ. Nhưng chủ quán không nấu được món nếu màn hình chỉ hiện "3 món",
 * nên màn chi tiết của merchant_app phải có đường lấy đủ dòng món. Thiếu route này thì
 * sprint 2.2 sẽ phải dựng màn chi tiết bằng dữ liệu tóm tắt rồi viết lại ở 2.3.
 */
@AutoConfigureMockMvc
class MerchantOrderDetailTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    private record Fixture(Tenant tenant, String token, String slug) {
    }

    private Fixture setup(String phone) throws Exception {
        String slug = "detail-" + UUID.randomUUID();
        Tenant tenant = tenantRepository.save(new Tenant(slug, "Quán test chi tiết đơn"));
        TenantContext.set(tenant.getId());
        try {
            merchantRepository.save(new Merchant(phone, passwordEncoder.encode("secret123"), "Chủ quán",
                    Merchant.MerchantRole.OWNER));
        } finally {
            TenantContext.clear();
        }

        String loginJson = mockMvc.perform(post("/v1/merchant/{slug}/auth/login", slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\",\"password\":\"secret123\"}".formatted(phone)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new Fixture(tenant, (String) readMap(loginJson).get("token"), slug);
    }

    /** Đặt đơn qua public plane để có dòng món THẬT, không seed tay entity. */
    private String datDonThat(Fixture fixture, int qty) throws Exception {
        UUID productId;
        TenantContext.set(fixture.tenant().getId());
        try {
            Category category = categoryRepository.save(new Category("Món chính", 1));
            productId = productRepository.save(new Product(category, "Bún chả", 45000, null, true)).getId();
        } finally {
            TenantContext.clear();
        }

        String orderJson = mockMvc.perform(post("/v1/s/{slug}/orders", fixture.slug())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":\"%s\",\"qty\":%d}]}".formatted(productId, qty)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return (String) readMap(orderJson).get("id");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        return objectMapper.readValue(json, Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> itemsOf(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("items");
    }

    private MvcResult detail(String token, String orderId) throws Exception {
        return mockMvc.perform(get("/v1/merchant/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
    }

    @Test
    void tra_ve_du_dong_mon_theo_snapshot() throws Exception {
        Fixture fixture = setup("0916000001");
        String orderId = datDonThat(fixture, 2);

        MvcResult result = detail(fixture.token(), orderId);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        Map<String, Object> body = readMap(result.getResponse().getContentAsString());
        assertThat(itemsOf(body)).hasSize(1);

        Map<String, Object> item = itemsOf(body).get(0);
        // Snapshot lúc đặt — bất biến #4, KHÔNG join lại products để hiển thị đơn cũ.
        assertThat(item.get("nameSnapshot")).isEqualTo("Bún chả");
        assertThat(item.get("unitPrice")).isEqualTo(45000);
        assertThat(item.get("qty")).isEqualTo(2);
        assertThat(item.get("lineTotal")).isEqualTo(90000);
        assertThat(body.get("total")).isEqualTo(90000);
    }

    @Test
    void khong_xem_duoc_don_cua_tenant_khac() throws Exception {
        Fixture a = setup("0916000002");
        Fixture b = setup("0916000003");
        String orderIdCuaA = datDonThat(a, 1);

        MvcResult result = detail(b.token(), orderIdCuaA);

        // 404 chứ không phải 403 — không xác nhận cho B biết đơn đó có tồn tại ở đâu đó.
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(readMap(result.getResponse().getContentAsString()).get("code")).isEqualTo("ORDER_NOT_FOUND");
    }

    @Test
    void don_khong_ton_tai_tra_404() throws Exception {
        Fixture fixture = setup("0916000004");

        MvcResult result = detail(fixture.token(), UUID.randomUUID().toString());
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void thieu_token_tra_401() throws Exception {
        Fixture fixture = setup("0916000005");
        String orderId = datDonThat(fixture, 1);

        MvcResult result = mockMvc.perform(get("/v1/merchant/orders/{id}", orderId)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    /**
     * Drift-guard cho định tuyến: {@code /sync} là literal, {@code /{id}} là template UUID.
     * Nếu Spring lỡ khớp {@code /sync} vào {@code /{id}} thì kênh nhận đơn của merchant_app
     * chết bằng lỗi ép kiểu UUID — hỏng thứ quan trọng nhất, vì một route mới thêm vào.
     */
    @Test
    void them_route_id_khong_lam_hong_route_sync() throws Exception {
        Fixture fixture = setup("0916000006");

        MvcResult result = mockMvc.perform(get("/v1/merchant/orders/sync")
                        .header("Authorization", "Bearer " + fixture.token()))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(readMap(result.getResponse().getContentAsString())).containsKey("serverTime");
    }
}
