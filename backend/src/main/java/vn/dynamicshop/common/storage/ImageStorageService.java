package vn.dynamicshop.common.storage;

/**
 * Adapter lưu ảnh — Stage 1 chuẩn bị hạ tầng cho R2/object storage (docs/70-stages.md
 * Stage 1: "Object storage cho ảnh (R2) + resize lúc upload"). Hai implementation:
 * {@link LocalDiskImageStorageService} (mặc định, giữ hành vi Stage 0 — lưu đĩa local) và
 * {@link S3ImageStorageService} (giao thức S3, chạy được với MinIO lẫn Cloudflare R2), chọn
 * qua property {@code app.storage.mode}.
 *
 * ⚠️ Tại thời điểm viết, CHƯA có HTTP endpoint nào gọi {@code upload()} — tính năng chụp ảnh
 * món ăn thuộc Stage 2 (merchant_app, xem docs/70-stages.md). {@code Product.imageUrl} hiện
 * là dữ liệu mock trỏ thẳng asset của Flutter, không phải ảnh do backend serve. Interface này
 * chỉ là hạ tầng cắm sẵn, để Stage 2 nối endpoint upload vào không phải build lại từ đầu.
 */
public interface ImageStorageService {

    /**
     * Ghi {@code content} vào vị trí {@code key} (đường dẫn tương đối, ví dụ
     * {@code "products/{tenantId}/{productId}.jpg"}) và trả về URL công khai để client dùng
     * ngay — tương đương gọi {@link #publicUrlOf(String)} sau khi ghi thành công.
     */
    String upload(String key, byte[] content, String contentType);

    /** URL công khai suy ra từ {@code key}, không cần ghi lại file (dùng khi đã biết key có sẵn). */
    String publicUrlOf(String key);
}
