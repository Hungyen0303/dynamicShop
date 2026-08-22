package vn.dynamicshop.common.storage;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

/**
 * {@code app.storage.mode=s3} — dùng SDK S3 chuẩn (giao thức chung cho AWS S3, MinIO, và
 * Cloudflare R2 — docs/70-stages.md Stage 1 chọn R2 cho production). {@code forcePathStyle}
 * bắt buộc bật vì MinIO/R2 không hỗ trợ virtual-hosted-style DNS như S3 thật.
 *
 * ⚠️ Chưa có endpoint HTTP nào gọi {@link #upload} — xem ghi chú ở {@link ImageStorageService}.
 * Test round-trip thật ở {@code S3ImageStorageServiceTest} dùng Testcontainers MinIO.
 */
@Service
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "s3")
public class S3ImageStorageService implements ImageStorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3ImageStorageService(StorageProperties properties) {
        StorageProperties.S3 s3Props = properties.getS3();
        this.bucket = s3Props.getBucket();
        this.publicBaseUrl = stripTrailingSlash(s3Props.getPublicBaseUrl());

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3Props.getRegion()))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3Props.getAccessKey(), s3Props.getSecretKey())));
        if (s3Props.getEndpoint() != null && !s3Props.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3Props.getEndpoint()));
        }
        this.s3Client = builder.build();
    }

    @Override
    public String upload(String key, byte[] content, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(content));
        return publicUrlOf(key);
    }

    @Override
    public String publicUrlOf(String key) {
        return publicBaseUrl + "/" + key;
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
