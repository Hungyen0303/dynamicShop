package vn.dynamicshop.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chọn implementation {@link FcmSender} lúc khởi động app. Quy tắc an toàn bắt buộc
 * (docs/70-stages.md Stage 1 + yêu cầu người): thiếu cấu hình Firebase KHÔNG BAO GIỜ được
 * làm app crash lúc khởi động — luôn fallback {@link LogOnlyFcmSender} một cách tự động, để
 * app chạy local y hệt Stage 0 khi chưa ai cấu hình gì.
 */
@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseFcmConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseFcmConfig.class);

    @Bean
    public FcmSender fcmSender(FirebaseProperties properties) {
        if (!properties.isEnabled()) {
            log.info("[fcm] app.firebase.enabled=false — dùng LogOnlyFcmSender (hành vi Stage 0, chưa nối Firebase)");
            return new LogOnlyFcmSender();
        }
        String rawPath = properties.getServiceAccountPath();
        if (rawPath == null || rawPath.isBlank()) {
            log.warn("[fcm] app.firebase.enabled=true nhưng thiếu app.firebase.service-account-path — "
                    + "fallback LogOnlyFcmSender, KHÔNG crash app");
            return new LogOnlyFcmSender();
        }
        Path path = Path.of(rawPath);
        if (!Files.exists(path)) {
            log.warn("[fcm] app.firebase.enabled=true nhưng không tìm thấy service-account tại '{}' — "
                    + "fallback LogOnlyFcmSender, KHÔNG crash app", rawPath);
            return new LogOnlyFcmSender();
        }
        try (InputStream credentialsStream = Files.newInputStream(path)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            log.info("[fcm] Firebase khởi tạo thành công từ '{}' — dùng FirebaseFcmSender thật", rawPath);
            return new FirebaseFcmSender(FirebaseMessaging.getInstance(app));
        } catch (IOException | RuntimeException e) {
            // RuntimeException bắt thêm lỗi credentials sai định dạng (GoogleCredentials ném
            // nhiều loại exception khác nhau tuỳ nội dung file hỏng) — vẫn KHÔNG crash app.
            log.error("[fcm] lỗi khởi tạo Firebase từ '{}' — fallback LogOnlyFcmSender, KHÔNG crash app: {}",
                    rawPath, e.getMessage());
            return new LogOnlyFcmSender();
        }
    }
}
