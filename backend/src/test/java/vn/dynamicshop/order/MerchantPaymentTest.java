package vn.dynamicshop.order;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Nút "Đã nhận tiền" — {@code POST /v1/merchant/orders/{id}/payment}.
 *
 * Hai điều được kiểm ở đây quan trọng hơn phần còn lại:
 *   1. Đổi {@code paymentStatus} KHÔNG kéo theo {@code orderStatus} (bất biến #5).
 *   2. Gửi lại đúng request với cùng {@code Idempotency-Key} KHÔNG ghi nhận thu tiền hai
 *      lần — merchant_app đẩy mọi hành động qua offline queue có retry, nên đây là đường
 *      đi thường xuyên chứ không phải trường hợp hiếm.
 */
@AutoConfigureMockMvc
class MerchantPaymentTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderEventRepository orderEventRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    private record Fixture(Tenant tenant, String token, UUID orderId) {
    }

    private Fixture setup(String phone) throws Exception {
        String slug = "pay-" + UUID.randomUUID();
        Tenant tenant = tenantRepository.save(new Tenant(slug, "Quán test thanh toán"));

        UUID orderId;
        TenantContext.set(tenant.getId());
        try {
            merchantRepository.save(new Merchant(phone, passwordEncoder.encode("secret123"), "Chủ quán",
                    Merchant.MerchantRole.OWNER));
            orderId = orderRepository.save(new Order("PAY-" + UUID.randomUUID(), null, null, null, "0900000000"))
                    .getId();
        } finally {
            TenantContext.clear();
        }

        String loginJson = mockMvc.perform(post("/v1/merchant/{slug}/auth/login", slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\",\"password\":\"secret123\"}".formatted(phone)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new Fixture(tenant, (String) readMap(loginJson).get("token"), orderId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        return objectMapper.readValue(json, Map.class);
    }

    private MvcResult markPaid(Fixture fixture, String idempotencyKey, String body) throws Exception {
        var request = post("/v1/merchant/orders/{id}/payment", fixture.orderId())
                .header("Authorization", "Bearer " + fixture.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (idempotencyKey != null) {
            request = request.header("Idempotency-Key", idempotencyKey);
        }
        return mockMvc.perform(request).andReturn();
    }

    @Test
    void danh_dau_da_nhan_tien_khong_dung_toi_order_status() throws Exception {
        Fixture fixture = setup("0912000001");

        MvcResult result = markPaid(fixture, UUID.randomUUID().toString(),
                "{\"to\":\"PAID\",\"reason\":\"khách trả tiền mặt\"}");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        Map<String, Object> body = readMap(result.getResponse().getContentAsString());
        assertThat(body.get("paymentStatus")).isEqualTo("PAID");
        // Hai trục độc lập — đơn vẫn đang PENDING, chưa ai xác nhận nó cả.
        assertThat(body.get("orderStatus")).isEqualTo("PENDING");

        TenantContext.set(fixture.tenant().getId());
        try {
            assertThat(orderRepository.findById(fixture.orderId()).orElseThrow().getPaymentStatus().name())
                    .isEqualTo("PAID");
            // Lịch sử ghi kèm tiền tố PAYMENT: để phân biệt với trục order_status.
            assertThat(orderEventRepository.findAll())
                    .anyMatch(e -> "PAYMENT:PAID".equals(e.getToStatus()));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void gui_lai_cung_idempotency_key_khong_ghi_nhan_thu_tien_hai_lan() throws Exception {
        Fixture fixture = setup("0912000002");
        String key = UUID.randomUUID().toString();
        String body = "{\"to\":\"PAID\",\"reason\":\"tiền mặt\"}";

        MvcResult first = markPaid(fixture, key, body);
        assertThat(first.getResponse().getStatus()).isEqualTo(200);

        // Lần hai: nếu KHÔNG có idempotency, state machine sẽ ném 409 (PAID không chuyển
        // sang PAID được) — nên 200 ở đây chứng minh nó thật sự phát lại response cũ chứ
        // không phải chạy lại hành động.
        MvcResult second = markPaid(fixture, key, body);
        assertThat(second.getResponse().getStatus()).isEqualTo(200);
        assertThat(second.getResponse().getContentAsString())
                .isEqualTo(first.getResponse().getContentAsString());

        TenantContext.set(fixture.tenant().getId());
        try {
            assertThat(orderEventRepository.findAll())
                    .filteredOn(e -> "PAYMENT:PAID".equals(e.getToStatus()))
                    .hasSize(1);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void thieu_idempotency_key_bi_tu_choi() throws Exception {
        Fixture fixture = setup("0912000003");

        MvcResult result = markPaid(fixture, null, "{\"to\":\"PAID\"}");
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(readMap(result.getResponse().getContentAsString()).get("code"))
                .isEqualTo("MISSING_IDEMPOTENCY_KEY");
    }

    @Test
    void cung_key_khac_noi_dung_tra_409() throws Exception {
        Fixture fixture = setup("0912000004");
        String key = UUID.randomUUID().toString();

        assertThat(markPaid(fixture, key, "{\"to\":\"PAID\"}").getResponse().getStatus()).isEqualTo(200);

        MvcResult conflict = markPaid(fixture, key, "{\"to\":\"REFUNDED\"}");
        assertThat(conflict.getResponse().getStatus()).isEqualTo(409);
        assertThat(readMap(conflict.getResponse().getContentAsString()).get("code"))
                .isEqualTo("IDEMPOTENCY_KEY_CONFLICT");
    }

    @Test
    void chuyen_payment_status_khong_hop_le_tra_409() throws Exception {
        Fixture fixture = setup("0912000005");

        // UNPAID → REFUNDED không có trong contracts/order-states.json.
        MvcResult result = markPaid(fixture, UUID.randomUUID().toString(), "{\"to\":\"REFUNDED\"}");
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(readMap(result.getResponse().getContentAsString()).get("code"))
                .isEqualTo("INVALID_PAYMENT_TRANSITION");
    }

    @Test
    void gia_tri_payment_status_khong_ton_tai_tra_400() throws Exception {
        Fixture fixture = setup("0912000006");

        MvcResult result = markPaid(fixture, UUID.randomUUID().toString(), "{\"to\":\"DA_TRA_ROI\"}");
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(readMap(result.getResponse().getContentAsString()).get("code"))
                .isEqualTo("INVALID_PAYMENT_STATUS_VALUE");
    }

    @Test
    void khong_danh_dau_duoc_don_cua_tenant_khac() throws Exception {
        Fixture a = setup("0912000007");
        Fixture b = setup("0912000008");

        // Token của B, orderId của A → 404 chứ không phải 403: không xác nhận đơn đó tồn tại.
        MvcResult result = mockMvc.perform(post("/v1/merchant/orders/{id}/payment", a.orderId())
                        .header("Authorization", "Bearer " + b.token())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"PAID\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(readMap(result.getResponse().getContentAsString()).get("code")).isEqualTo("ORDER_NOT_FOUND");
    }
}
