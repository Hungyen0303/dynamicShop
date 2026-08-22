package vn.dynamicshop.common.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Mặc định Stage 0/1 ({@code app.storage.mode=local} hoặc không cấu hình gì) — ghi ảnh vào
 * thư mục đĩa local, y hệt tinh thần "Ảnh ở Stage 0: lưu vào thư mục local, Spring serve
 * tĩnh" (docs/70-stages.md). Lưu ý: KHÔNG có endpoint HTTP nào gọi {@link #upload} tại thời
 * điểm viết — xem {@link ImageStorageService}. Class này chỉ đảm bảo khi Stage 2 nối endpoint
 * upload thật, ghi/đọc file hoạt động đúng ngay.
 */
@Service
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "local", matchIfMissing = true)
public class LocalDiskImageStorageService implements ImageStorageService {

    private final Path baseDir;
    private final String publicBaseUrl;

    public LocalDiskImageStorageService(StorageProperties properties) {
        this.baseDir = Path.of(properties.getLocal().getDir()).toAbsolutePath().normalize();
        this.publicBaseUrl = stripTrailingSlash(properties.getLocal().getPublicBaseUrl());
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Không tạo được thư mục lưu ảnh: " + baseDir, e);
        }
    }

    @Override
    public String upload(String key, byte[] content, String contentType) {
        Path target = resolveWithinBaseDir(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Không ghi được ảnh: " + key, e);
        }
        return publicUrlOf(key);
    }

    @Override
    public String publicUrlOf(String key) {
        return publicBaseUrl + "/" + key;
    }

    /** Chặn path traversal (key kiểu {@code "../../etc/passwd"}) — key luôn phải nằm trong baseDir. */
    private Path resolveWithinBaseDir(String key) {
        Path target = baseDir.resolve(key).normalize();
        if (!target.startsWith(baseDir)) {
            throw new IllegalArgumentException("Key ảnh không hợp lệ (path traversal): " + key);
        }
        return target;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
