package vn.dynamicshop.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation thật — gọi Firebase Cloud Messaging. Chỉ được tạo bean khi
 * {@link FirebaseFcmConfig} xác nhận có {@code app.firebase.enabled=true} VÀ file
 * service-account tồn tại (nghĩa là chủ dự án đã có Firebase project thật — Stage 1).
 */
public class FirebaseFcmSender implements FcmSender {

    private static final Logger log = LoggerFactory.getLogger(FirebaseFcmSender.class);

    private final FirebaseMessaging messaging;

    public FirebaseFcmSender(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    @Override
    public void send(String deviceToken, String title, String body, Map<String, Object> data) {
        if (deviceToken == null || deviceToken.isBlank()) {
            // Stage 1 chưa có bảng đăng ký device token merchant (Stage 2) — không phải lỗi,
            // chỉ là "chưa biết gửi cho thiết bị nào".
            log.debug("[fcm] bỏ qua gửi — chưa có deviceToken. title={}", title);
            return;
        }
        Message.Builder messageBuilder = Message.builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build());
        data.forEach((key, value) -> messageBuilder.putData(key, String.valueOf(value)));
        try {
            String messageId = messaging.send(messageBuilder.build());
            log.info("[fcm] gửi thành công messageId={} token={}", messageId, mask(deviceToken));
        } catch (FirebaseMessagingException e) {
            // Không throw tiếp — at-least-once ở tầng outbox, retry được ở lần poll sau nếu
            // cần; một token chết không được làm crash cả batch xử lý outbox.
            log.error("[fcm] gửi thất bại token={} lý do={}", mask(deviceToken), e.getMessage());
        }
    }

    private static String mask(String token) {
        if (token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
