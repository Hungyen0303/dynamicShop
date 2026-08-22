package vn.dynamicshop.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import vn.dynamicshop.AbstractIntegrationTest;

/**
 * {@code app.firebase.enabled=true} nhưng KHÔNG có file service-account thật ở đường dẫn cấu
 * hình — vẫn phải fallback {@link LogOnlyFcmSender} một cách tự động, KHÔNG crash app lúc
 * khởi động (yêu cầu bắt buộc: thiếu credential thật không được chặn chạy local).
 */
class FcmFallbackWhenMissingServiceAccountTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void firebaseProperties(DynamicPropertyRegistry registry) {
        registry.add("app.firebase.enabled", () -> "true");
        registry.add("app.firebase.service-account-path", () -> "/khong-ton-tai/service-account.json");
    }

    @Autowired
    private FcmSender fcmSender;

    @Test
    void enabled_true_nhung_thieu_file_van_fallback_log_only() {
        assertThat(fcmSender).isInstanceOf(LogOnlyFcmSender.class);
    }
}
