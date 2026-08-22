package vn.dynamicshop.notification;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fallback an toàn — dùng khi {@code app.firebase.enabled=false} (mặc định) HOẶC không tìm
 * thấy file service-account ở đường dẫn cấu hình (xem {@link FirebaseFcmConfig}). KHÔNG BAO
 * GIỜ throw, KHÔNG BAO GIỜ gọi Firebase SDK thật — đây chính là hành vi Stage 0 ("outbox
 * worker chỉ ghi log", docs/70-stages.md), giữ nguyên sau khi tách qua {@link FcmSender}.
 */
public class LogOnlyFcmSender implements FcmSender {

    private static final Logger log = LoggerFactory.getLogger(LogOnlyFcmSender.class);

    @Override
    public void send(String deviceToken, String title, String body, Map<String, Object> data) {
        log.info("[fcm] log-only (chưa cấu hình Firebase) — token={} title={} body={} data={}",
                deviceToken, title, body, data);
    }
}
