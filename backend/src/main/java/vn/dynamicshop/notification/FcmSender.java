package vn.dynamicshop.notification;

import java.util.Map;

/**
 * Bất biến #7 (docs/30-backend.md) — chỉ được gọi từ {@code OutboxBatchProcessor}, KHÔNG BAO
 * GIỜ trong transaction nghiệp vụ tạo/đổi đơn. Hai implementation: {@link FirebaseFcmSender}
 * (thật, cần service-account) và {@link LogOnlyFcmSender} (fallback an toàn khi chưa cấu
 * hình Firebase — hành vi Stage 0). Bean nào được dùng chọn ở {@link FirebaseFcmConfig}.
 */
public interface FcmSender {

    /**
     * @param deviceToken FCM registration token của thiết bị merchant, lấy từ bảng
     *                    {@code device_tokens} của ĐÚNG tenant đang xử lý (sprint 2.1).
     *                    Vẫn có thể {@code null} — implementation phải xử lý an toàn
     *                    (không throw) khi thiếu token, vì bất biến "không bao giờ crash vì
     *                    thiếu cấu hình" quan trọng hơn việc gửi được một push.
     */
    void send(String deviceToken, String title, String body, Map<String, Object> data);
}
