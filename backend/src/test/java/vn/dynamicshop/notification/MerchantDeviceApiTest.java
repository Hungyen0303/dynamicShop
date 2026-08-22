package vn.dynamicshop.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * {@code POST/DELETE /v1/merchant/devices} — merchant_app đăng ký FCM token sau khi đăng
 * nhập, thu hồi khi đăng xuất.
 *
 * Không cần Firebase thật để test đường này: token là chuỗi opaque với backend, nên token
 * giả chạy qua đúng cùng một đường mã như token thật. Đó là lý do sprint 2.1 làm được trọn
 * vẹn trong khi mục 3/mục 6 của {@code missing_config.md} vẫn còn trống.
 */
@AutoConfigureMockMvc
class MerchantDeviceApiTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private DeviceTokenRepository deviceTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    private record Shop(Tenant tenant, String token) {
    }

    private Shop createShopWithLogin(String phone) throws Exception {
        String slug = "dev-" + UUID.randomUUID();
        Tenant tenant = tenantRepository.save(new Tenant(slug, "Quán test device"));
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
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(loginJson, Map.class);
        return new Shop(tenant, (String) parsed.get("token"));
    }

    private MvcResult register(Shop shop, String body) throws Exception {
        return mockMvc.perform(post("/v1/merchant/devices")
                        .header("Authorization", "Bearer " + shop.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    @Test
    void dang_ky_token_moi_thanh_cong_va_khong_tra_lai_token() throws Exception {
        Shop shop = createShopWithLogin("0913000001");
        String fcmToken = "fake-fcm-" + UUID.randomUUID();

        MvcResult result = register(shop,
                "{\"token\":\"%s\",\"platform\":\"ANDROID\",\"appVersion\":\"1.0.0\"}".formatted(fcmToken));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        // Token không quay lại trong response — client vừa gửi nó lên, không có lý do gì để
        // nó xuất hiện thêm một lần nữa trong log/proxy trên đường về.
        assertThat(result.getResponse().getContentAsString()).doesNotContain(fcmToken);

        TenantContext.set(shop.tenant().getId());
        try {
            assertThat(deviceTokenRepository.findByToken(fcmToken)).isPresent();
            assertThat(deviceTokenRepository.findByRevokedAtIsNull()).hasSize(1);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void dang_ky_lai_cung_token_khong_tao_dong_thu_hai() throws Exception {
        Shop shop = createShopWithLogin("0913000002");
        String fcmToken = "fake-fcm-" + UUID.randomUUID();
        String body = "{\"token\":\"%s\",\"platform\":\"ANDROID\",\"appVersion\":\"1.0.0\"}".formatted(fcmToken);

        assertThat(register(shop, body).getResponse().getStatus()).isEqualTo(200);
        assertThat(register(shop, body).getResponse().getStatus()).isEqualTo(200);

        TenantContext.set(shop.tenant().getId());
        try {
            // Tạo dòng mới mỗi lần đăng nhập sẽ khiến outbox worker gửi trùng lên cùng một máy.
            assertThat(deviceTokenRepository.findByRevokedAtIsNull()).hasSize(1);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void dang_xuat_thu_hoi_token() throws Exception {
        Shop shop = createShopWithLogin("0913000003");
        String fcmToken = "fake-fcm-" + UUID.randomUUID();
        register(shop, "{\"token\":\"%s\",\"platform\":\"ANDROID\"}".formatted(fcmToken));

        mockMvc.perform(delete("/v1/merchant/devices")
                        .header("Authorization", "Bearer " + shop.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(fcmToken)))
                .andExpect(status().isNoContent());

        TenantContext.set(shop.tenant().getId());
        try {
            assertThat(deviceTokenRepository.findByRevokedAtIsNull()).isEmpty();
            // Soft delete — dòng còn đó để sau này tra "máy nào từng nhận đơn" khi có sự cố sót đơn.
            assertThat(deviceTokenRepository.findByToken(fcmToken)).isPresent();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void thu_hoi_token_khong_ton_tai_van_thanh_cong() throws Exception {
        Shop shop = createShopWithLogin("0913000004");

        // Đăng xuất phải luôn thành công phía người dùng — "token này vốn không có ở đây"
        // chính là trạng thái mà lời gọi muốn đạt tới.
        mockMvc.perform(delete("/v1/merchant/devices")
                        .header("Authorization", "Bearer " + shop.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"chua-tung-dang-ky\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void tenant_khac_khong_thay_token_cua_nhau() throws Exception {
        Shop a = createShopWithLogin("0913000005");
        Shop b = createShopWithLogin("0913000006");
        String tokenA = "fake-fcm-A-" + UUID.randomUUID();
        register(a, "{\"token\":\"%s\",\"platform\":\"ANDROID\"}".formatted(tokenA));

        TenantContext.set(b.tenant().getId());
        try {
            assertThat(deviceTokenRepository.findByToken(tokenA)).isEmpty();
            assertThat(deviceTokenRepository.findByRevokedAtIsNull()).isEmpty();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void platform_khong_hop_le_tra_400() throws Exception {
        Shop shop = createShopWithLogin("0913000007");

        MvcResult result = register(shop, "{\"token\":\"abc\",\"platform\":\"SYMBIAN\"}");
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertThat(body.get("code")).isEqualTo("INVALID_PLATFORM");
    }

    @Test
    void khong_co_token_dang_nhap_thi_khong_dang_ky_duoc() throws Exception {
        mockMvc.perform(post("/v1/merchant/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc\",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isForbidden());
    }
}
