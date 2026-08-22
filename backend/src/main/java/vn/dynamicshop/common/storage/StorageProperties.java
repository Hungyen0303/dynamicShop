package vn.dynamicshop.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.storage.*} — xem {@link StorageConfig}. Mặc định {@code mode=local} giữ nguyên
 * hành vi Stage 0 (không cấu hình gì thêm thì app chạy y hệt trước).
 */
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** {@code local} (mặc định) hoặc {@code s3}. */
    private String mode = "local";

    private Local local = new Local();
    private S3 s3 = new S3();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }

    public static class Local {
        /** Thư mục đĩa local để ghi ảnh — Spring chưa serve tĩnh thư mục này (xem ImageStorageService). */
        private String dir = "./data/images";
        /** Tiền tố URL công khai giả định — chưa có route thật phục vụ nó ở Stage 0/1. */
        private String publicBaseUrl = "http://localhost:8080/static/images";

        public String getDir() {
            return dir;
        }

        public void setDir(String dir) {
            this.dir = dir;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }

    public static class S3 {
        /** Endpoint S3-compatible — MinIO local hoặc R2 thật. Trống = dùng endpoint AWS mặc định của SDK. */
        private String endpoint;
        private String bucket;
        private String accessKey;
        private String secretKey;
        /** R2 dùng {@code "auto"}; MinIO/AWS dùng region thật. */
        private String region = "auto";
        /** Tiền tố URL công khai để build link ảnh trả về client (CDN/public bucket domain). */
        private String publicBaseUrl;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }
}
