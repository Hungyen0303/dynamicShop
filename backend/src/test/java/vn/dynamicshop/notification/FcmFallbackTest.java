package vn.dynamicshop.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.dynamicshop.AbstractIntegrationTest;

/**
 * Bất biến an toàn bắt buộc: khi {@code app.firebase.enabled=false} (mặc định, KHÔNG cấu
 * hình gì thêm — {@code application.yml}), app phải khởi động bình thường và dùng
 * {@link LogOnlyFcmSender}, KHÔNG BAO GIỜ gọi Firebase SDK thật.
 */
class FcmFallbackTest extends AbstractIntegrationTest {

    @Autowired
    private FcmSender fcmSender;

    @Test
    void mac_dinh_khong_cau_hinh_gi_thi_dung_log_only_sender() {
        assertThat(fcmSender).isInstanceOf(LogOnlyFcmSender.class);
    }

    @Test
    void log_only_sender_khong_throw_du_thieu_du_lieu() {
        // Không có deviceToken (Stage 1 chưa có bảng đăng ký thiết bị) — vẫn phải an toàn.
        fcmSender.send(null, "title", "body", java.util.Map.of("orderId", "abc"));
    }
}
