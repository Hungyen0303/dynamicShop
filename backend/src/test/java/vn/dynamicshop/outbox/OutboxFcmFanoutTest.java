package vn.dynamicshop.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.dynamicshop.AbstractIntegrationTest;
import vn.dynamicshop.common.tenant.Tenant;
import vn.dynamicshop.common.tenant.TenantContext;
import vn.dynamicshop.common.tenant.TenantRepository;
import vn.dynamicshop.merchant.Merchant;
import vn.dynamicshop.merchant.MerchantRepository;
import vn.dynamicshop.notification.DeviceToken;
import vn.dynamicshop.notification.DeviceTokenRepository;
import vn.dynamicshop.notification.FcmSender;

/**
 * 🔴 Test đắt giá nhất của sprint 2.1. Nó kiểm đúng chỗ mà một lỗi sẽ giết cả sản phẩm:
 * outbox worker duyệt LẦN LƯỢT từng tenant, và nếu việc resolve device token không bám
 * đúng tenant đang xử lý thì đơn của quán này sẽ kêu chuông trên điện thoại của quán khác.
 * Chủ quán ở tỉnh biết nhau hết — một lần như vậy là mất cả tỉnh
 * (.claude/skills/tenant-isolation).
 *
 * Kiểm được TOÀN BỘ đường ống "đơn mới → outbox → worker → resolve token đúng tenant → gọi
 * sender" mà KHÔNG cần Firebase thật, nhờ thay {@link FcmSender} bằng bản ghi lại lời gọi.
 * Đây chính là phần mà {@code progress.md} mục 9 gọi là "làm được đàng hoàng không cần
 * credential".
 */
class OutboxFcmFanoutTest extends AbstractIntegrationTest {

    /** Ghi lại lời gọi thay vì gửi thật — không đụng tới Firebase SDK. */
    static class RecordingFcmSender implements FcmSender {
        final List<String> sentTokens = new CopyOnWriteArrayList<>();
        final List<String> sentTitles = new CopyOnWriteArrayList<>();

        @Override
        public void send(String deviceToken, String title, String body, Map<String, Object> data) {
            sentTokens.add(deviceToken);
            sentTitles.add(title);
        }
    }

    @TestConfiguration
    static class RecordingFcmConfig {
        @Bean
        @Primary
        RecordingFcmSender recordingFcmSender() {
            return new RecordingFcmSender();
        }
    }

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private DeviceTokenRepository deviceTokenRepository;
    @Autowired
    private OutboxPublisher outboxPublisher;
    @Autowired
    private OutboxWorker outboxWorker;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RecordingFcmSender recordingFcmSender;
    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void resetRecorder() {
        recordingFcmSender.sentTokens.clear();
        recordingFcmSender.sentTitles.clear();
    }

    private Tenant createShopWithDevices(String phone, String... fcmTokens) {
        Tenant tenant = tenantRepository.save(new Tenant("fanout-" + UUID.randomUUID(), "Quán test fanout"));
        TenantContext.set(tenant.getId());
        try {
            Merchant merchant = merchantRepository.save(new Merchant(phone, passwordEncoder.encode("x"), "Chủ",
                    Merchant.MerchantRole.OWNER));
            for (String fcmToken : fcmTokens) {
                deviceTokenRepository.save(
                        new DeviceToken(merchant.getId(), fcmToken, DeviceToken.Platform.ANDROID, "1.0.0"));
            }
        } finally {
            TenantContext.clear();
        }
        return tenant;
    }

    private void enqueueNewOrder(Tenant tenant) {
        TenantContext.set(tenant.getId());
        try {
            outboxPublisher.enqueue(UUID.randomUUID(), "NEW_ORDER",
                    Map.of("code", "OD-FANOUT", "total", 50000));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void gui_toi_moi_thiet_bi_con_song_cua_dung_tenant() {
        String tokenMayChu = "fanout-chu-" + UUID.randomUUID();
        String tokenMayNhanVien = "fanout-nv-" + UUID.randomUUID();
        Tenant tenant = createShopWithDevices("0914000001", tokenMayChu, tokenMayNhanVien);

        enqueueNewOrder(tenant);
        outboxWorker.pollAndLog();

        // Một quán thường có nhiều máy — chuông phải kêu ở tất cả, không chỉ máy đăng ký sau cùng.
        assertThat(recordingFcmSender.sentTokens).contains(tokenMayChu, tokenMayNhanVien);
        assertThat(recordingFcmSender.sentTitles).contains("Đơn hàng mới");
    }

    @Test
    void khong_gui_push_sang_thiet_bi_cua_tenant_khac() {
        String tokenA = "fanout-A-" + UUID.randomUUID();
        String tokenB = "fanout-B-" + UUID.randomUUID();
        Tenant a = createShopWithDevices("0914000002", tokenA);
        createShopWithDevices("0914000003", tokenB);

        // CHỈ tenant A có đơn mới.
        enqueueNewOrder(a);
        outboxWorker.pollAndLog();

        assertThat(recordingFcmSender.sentTokens).contains(tokenA);
        // ← dòng đáng giá nhất cả file: máy của quán B tuyệt đối không được rung.
        assertThat(recordingFcmSender.sentTokens).doesNotContain(tokenB);
    }

    @Test
    void thiet_bi_da_thu_hoi_khong_con_nhan_push() {
        String tokenConSong = "fanout-live-" + UUID.randomUUID();
        String tokenDaThuHoi = "fanout-revoked-" + UUID.randomUUID();
        Tenant tenant = createShopWithDevices("0914000004", tokenConSong, tokenDaThuHoi);

        TenantContext.set(tenant.getId());
        try {
            // Phải save() tường minh: transaction của findByToken đã đóng khi nó trả về, nên
            // entity ở đây đã detached — sửa field trên nó không có dirty checking nào bắt
            // được, và test sẽ xanh giả trong khi DB không đổi gì.
            DeviceToken revoked = deviceTokenRepository.findByToken(tokenDaThuHoi).orElseThrow();
            revoked.revoke();
            deviceTokenRepository.save(revoked);
        } finally {
            TenantContext.clear();
        }

        enqueueNewOrder(tenant);
        outboxWorker.pollAndLog();

        assertThat(recordingFcmSender.sentTokens).contains(tokenConSong);
        // Nhân viên nghỉ việc, đăng xuất khỏi máy mình — máy đó không được nhận đơn nữa.
        assertThat(recordingFcmSender.sentTokens).doesNotContain(tokenDaThuHoi);
    }

    @Test
    void quan_chua_cai_app_van_xu_ly_outbox_binh_thuong() {
        Tenant tenant = createShopWithDevices("0914000005");

        enqueueNewOrder(tenant);
        outboxWorker.pollAndLog();

        // Không có máy nào để gửi KHÔNG phải lỗi, và tuyệt đối không được làm kẹt hàng đợi
        // outbox của cả hệ thống.
        TenantContext.set(tenant.getId());
        try {
            assertThat(outboxRepository.findAll()).allMatch(e -> e.getProcessedAt() != null);
        } finally {
            TenantContext.clear();
        }
    }
}
