package vn.dynamicshop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import vn.dynamicshop.common.tenant.Tenant;
import vn.dynamicshop.common.tenant.TenantContext;
import vn.dynamicshop.common.tenant.TenantRepository;
import vn.dynamicshop.merchant.Merchant;
import vn.dynamicshop.merchant.MerchantRepository;

/**
 * {@code GET /v1/merchant/orders/sync} — kênh nhận đơn FOREGROUND của merchant_app (bất
 * biến #1, docs/11-merchant-app.md). Endpoint này không phụ thuộc Firebase, nên nó test
 * được đầy đủ ngay cả khi credential FCM thật còn treo (progress.md mục 9).
 *
 * Test qua HTTP thật (MockMvc) chứ không gọi thẳng service: phần dễ sai nhất nằm ở tầng
 * HTTP — ETag/304, tenant lấy từ JWT, mã lỗi — chứ không phải ở câu query.
 */
@AutoConfigureMockMvc
class MerchantOrderSyncTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    private record Shop(Tenant tenant, String token) {
    }

    private Shop createShopWithLogin(String phone) throws Exception {
        String slug = "sync-" + UUID.randomUUID();
        Tenant tenant = tenantRepository.save(new Tenant(slug, "Quán test sync"));
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
        return new Shop(tenant, (String) readMap(loginJson).get("token"));
    }

    private void seedOrders(Tenant tenant, int count, String prefix) {
        TenantContext.set(tenant.getId());
        try {
            for (int i = 0; i < count; i++) {
                orderRepository.save(new Order(prefix + "-" + i, null, null, null, "090000000" + i));
            }
        } finally {
            TenantContext.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        return objectMapper.readValue(json, Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> ordersOf(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("orders");
    }

    private MvcResult sync(String token, String queryAndHeaders) throws Exception {
        return mockMvc.perform(get("/v1/merchant/orders/sync" + queryAndHeaders)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
    }

    @Test
    void tra_ve_don_cua_dung_tenant_kem_so_luong_mon() throws Exception {
        Shop shop = createShopWithLogin("0911000001");
        seedOrders(shop.tenant(), 3, "SYNC-A");

        MvcResult result = sync(shop.token(), "");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        Map<String, Object> body = readMap(result.getResponse().getContentAsString());
        assertThat(ordersOf(body)).hasSize(3);
        assertThat(body).containsKeys("serverTime", "hasMore");
        assertThat(body.get("hasMore")).isEqualTo(false);

        // Shape đúng như docs/90-api-contract.md — không có "items" trong bản tóm tắt.
        Map<String, Object> first = ordersOf(body).get(0);
        assertThat(first).containsKeys("id", "code", "orderStatus", "paymentStatus", "total", "itemCount",
                "createdAt", "updatedAt");
        assertThat(first).doesNotContainKey("items");
        assertThat(first.get("itemCount")).isEqualTo(0);
    }

    @Test
    void merchant_khong_thay_don_cua_tenant_khac() throws Exception {
        Shop a = createShopWithLogin("0911000002");
        Shop b = createShopWithLogin("0911000003");
        seedOrders(a.tenant(), 4, "SYNC-ONLY-A");
        seedOrders(b.tenant(), 2, "SYNC-ONLY-B");

        Map<String, Object> bodyB = readMap(sync(b.token(), "").getResponse().getContentAsString());

        // Đây là dòng đáng giá nhất của cả file: token của B, tenant lấy từ JWT claim,
        // KHÔNG được thấy một đơn nào của A dù hai tenant sống chung một bảng.
        assertThat(ordersOf(bodyB)).hasSize(2);
        assertThat(ordersOf(bodyB)).allMatch(o -> String.valueOf(o.get("code")).startsWith("SYNC-ONLY-B"));
    }

    @Test
    void since_chi_tra_ve_don_moi_hon() throws Exception {
        Shop shop = createShopWithLogin("0911000004");
        seedOrders(shop.tenant(), 2, "SYNC-OLD");
        seedOrders(shop.tenant(), 3, "SYNC-NEW");

        Map<String, Object> all = readMap(sync(shop.token(), "").getResponse().getContentAsString());
        assertThat(ordersOf(all)).hasSize(5);

        // Mốc = updatedAt của đơn SYNC-NEW đầu tiên. Dùng >= nên chính nó vẫn nằm trong kết
        // quả (xem Javadoc OrderRepository) → còn đúng 3 đơn mới.
        String since = ordersOf(all).stream()
                .filter(o -> String.valueOf(o.get("code")).startsWith("SYNC-NEW"))
                .map(o -> String.valueOf(o.get("updatedAt")))
                .findFirst().orElseThrow();

        Map<String, Object> delta = readMap(sync(shop.token(), "?since=" + since)
                .getResponse().getContentAsString());
        assertThat(ordersOf(delta)).hasSize(3);
        assertThat(ordersOf(delta)).allMatch(o -> String.valueOf(o.get("code")).startsWith("SYNC-NEW"));
    }

    /**
     * 🔴 Sprint 2.1b — chốt chặn cho lỗi MẤT ĐƠN IM LẶNG lọt qua sprint 2.1.
     *
     * Bản cũ trả {@code Instant.now()} lấy SAU khi query xong. Đơn được
     * {@code @UpdateTimestamp} gán {@code updated_at = T1} nhưng commit ở T3; poll ở T2
     * (T1 &lt; T2 &lt; T3) không thấy đơn đó nhưng vẫn trả về mốc T2, và lần poll sau lọc
     * {@code >= T2} sẽ bỏ qua nó vĩnh viễn.
     *
     * Không mô phỏng được bằng đồng thời thật ở đây ({@code maximum-pool-size=1} trong
     * AbstractIntegrationTest sẽ deadlock nếu giữ một transaction mở rồi query từ connection
     * khác), nên kiểm thẳng BẤT BIẾN: mốc trả về phải nằm hẳn trong quá khứ so với thời
     * điểm gọi. Bản cũ đỏ ngay ở dòng này.
     */
    @Test
    void server_time_luon_lui_ve_qua_khu_du_xa_de_khong_bo_sot_don() throws Exception {
        Shop shop = createShopWithLogin("0911000009");

        Instant truocKhiGoi = Instant.now();
        Map<String, Object> body = readMap(sync(shop.token(), "").getResponse().getContentAsString());
        Instant serverTime = Instant.parse(String.valueOf(body.get("serverTime")));

        // Biên an toàn thật là 60s; kiểm 30s để test không mong manh nếu sau này siết lại,
        // nhưng vẫn đỏ chắc chắn với bản cũ (mốc nằm SAU thời điểm gọi).
        assertThat(serverTime).isBefore(truocKhiGoi.minusSeconds(30));
    }

    /**
     * Mặt hành vi của cùng bản sửa trên, viết theo đúng cách merchant_app sẽ dùng: lấy
     * {@code serverTime} của lần poll này làm {@code since} cho lần poll sau. Đơn vừa tạo
     * PHẢI còn nằm trong kết quả — cửa sổ chồng lấn chính là thứ che cho khe commit.
     *
     * Bản cũ đỏ: mốc trả về mới hơn {@code updated_at} của đơn nên nó biến mất ngay lần
     * poll thứ hai. Client nhận lặp là chấp nhận được vì nó BẮT BUỘC dedupe theo id rồi
     * (bất biến #4).
     */
    @Test
    void don_vua_tao_van_con_o_lan_poll_sau_khi_dung_server_time_lam_since() throws Exception {
        Shop shop = createShopWithLogin("0911000010");
        seedOrders(shop.tenant(), 1, "SYNC-WATERMARK");

        Map<String, Object> lanDau = readMap(sync(shop.token(), "").getResponse().getContentAsString());
        assertThat(ordersOf(lanDau)).hasSize(1);
        String since = String.valueOf(lanDau.get("serverTime"));

        Map<String, Object> lanSau = readMap(sync(shop.token(), "?since=" + since)
                .getResponse().getContentAsString());

        assertThat(ordersOf(lanSau))
                .as("đơn rơi khỏi kết quả ngay lần poll thứ hai = mất đơn vĩnh viễn ở production")
                .hasSize(1);
        assertThat(ordersOf(lanSau).get(0).get("code")).isEqualTo("SYNC-WATERMARK-0");
    }

    @Test
    void etag_giong_nhau_thi_tra_304_va_khong_co_body() throws Exception {
        Shop shop = createShopWithLogin("0911000005");
        seedOrders(shop.tenant(), 2, "SYNC-ETAG");

        MvcResult first = sync(shop.token(), "");
        String etag = first.getResponse().getHeader("ETag");
        assertThat(etag).isNotBlank();

        MvcResult second = mockMvc.perform(get("/v1/merchant/orders/sync")
                        .header("Authorization", "Bearer " + shop.token())
                        .header("If-None-Match", etag))
                .andReturn();

        assertThat(second.getResponse().getStatus()).isEqualTo(304);
        // Mục tiêu của cả cơ chế này: lần poll không có gì mới gần như không tốn byte nào
        // (docs/30-backend.md "phần lớn lần poll phải tốn ~200 byte").
        assertThat(second.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    void poll_rong_van_tra_304_khi_khong_co_don_nao_moi() throws Exception {
        Shop shop = createShopWithLogin("0911000006");

        String since = Instant.now().toString();
        MvcResult first = sync(shop.token(), "?since=" + since);
        assertThat(ordersOf(readMap(first.getResponse().getContentAsString()))).isEmpty();

        MvcResult second = mockMvc.perform(get("/v1/merchant/orders/sync")
                        .param("since", since)
                        .header("Authorization", "Bearer " + shop.token())
                        .header("If-None-Match", first.getResponse().getHeader("ETag")))
                .andReturn();
        assertThat(second.getResponse().getStatus()).isEqualTo(304);
    }

    @Test
    void qua_50_don_thi_cat_trang_va_bao_has_more() throws Exception {
        Shop shop = createShopWithLogin("0911000007");
        seedOrders(shop.tenant(), 51, "SYNC-BIG");

        Map<String, Object> body = readMap(sync(shop.token(), "").getResponse().getContentAsString());

        // Trần cứng do server giữ — client không có cách nào xin nhiều hơn.
        assertThat(ordersOf(body)).hasSize(50);
        assertThat(body.get("hasMore")).isEqualTo(true);
    }

    @Test
    void since_sai_dinh_dang_tra_400_co_ma_loi_ro_rang() throws Exception {
        Shop shop = createShopWithLogin("0911000008");

        MvcResult result = sync(shop.token(), "?since=hom-qua");
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(readMap(result.getResponse().getContentAsString()).get("code")).isEqualTo("INVALID_SINCE");
    }

    @Test
    void thieu_token_tra_401_khong_phai_403() throws Exception {
        // Sprint 2.1b: trước đây Spring trả 403 mặc định. Đổi thành 401 vì quyết định #12
        // dựng vòng đời phiên trên luật "gặp 401 thì tự đăng nhập lại" — 403 rơi ngoài luật
        // đó và biến một lỗi rơi header thành im lặng ngừng nhận đơn.
        MvcResult result = mockMvc.perform(get("/v1/merchant/orders/sync")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(readMap(result.getResponse().getContentAsString()).get("code")).isEqualTo("UNAUTHENTICATED");
    }

    @Test
    void token_hong_cung_tra_401_cung_shape() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/merchant/orders/sync")
                        .header("Authorization", "Bearer khong-phai-jwt"))
                .andReturn();

        // Cùng shape với trường hợp thiếu header — client chỉ cần nhớ đúng MỘT luật.
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(readMap(result.getResponse().getContentAsString()).get("code")).isEqualTo("UNAUTHENTICATED");
    }
}
