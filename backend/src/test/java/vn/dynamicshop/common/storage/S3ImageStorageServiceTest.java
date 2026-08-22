package vn.dynamicshop.common.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;

/**
 * Test round-trip THẬT cho {@code app.storage.mode=s3} — dùng MinIO qua Testcontainers (cùng
 * giao thức S3 với Cloudflare R2, xem docs/70-stages.md Stage 1). KHÔNG thêm MinIO vào
 * infra/docker/docker-compose.yml — container này CHỈ sống trong vòng đời test.
 */
@Testcontainers
class S3ImageStorageServiceTest {

    private static final String BUCKET = "ds-test-bucket";

    static MinIOContainer minio = new MinIOContainer("minio/minio:RELEASE.2024-01-16T16-07-38Z");

    @BeforeAll
    static void startAndPrepareBucket() {
        minio.start();
        try (S3Client bootstrapClient = S3Client.builder()
                .endpointOverride(URI.create(minio.getS3URL()))
                .region(Region.of("auto"))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(minio.getUserName(), minio.getPassword())))
                .build()) {
            bootstrapClient.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
            // Bucket MinIO mặc định private — ảnh sản phẩm phải đọc công khai qua HTTP không auth
            // (đây là hành vi thật của R2 khi bật "public access" cho bucket ở production, không
            // phải riêng của test). Set policy ngay ở đây để test round-trip phản ánh đúng thật.
            bootstrapClient.putBucketPolicy(PutBucketPolicyRequest.builder()
                    .bucket(BUCKET)
                    .policy("""
                            {
                              "Version": "2012-10-17",
                              "Statement": [{
                                "Effect": "Allow",
                                "Principal": "*",
                                "Action": "s3:GetObject",
                                "Resource": "arn:aws:s3:::%s/*"
                              }]
                            }
                            """.formatted(BUCKET))
                    .build());
        }
    }

    @AfterAll
    static void stop() {
        minio.stop();
    }

    private S3ImageStorageService newService() {
        StorageProperties properties = new StorageProperties();
        properties.setMode("s3");
        StorageProperties.S3 s3 = properties.getS3();
        s3.setEndpoint(minio.getS3URL());
        s3.setBucket(BUCKET);
        s3.setAccessKey(minio.getUserName());
        s3.setSecretKey(minio.getPassword());
        s3.setRegion("auto");
        s3.setPublicBaseUrl(minio.getS3URL() + "/" + BUCKET);
        return new S3ImageStorageService(properties);
    }

    @Test
    void upload_roundtrip_noi_dung_dung_qua_http() throws Exception {
        S3ImageStorageService service = newService();
        byte[] content = "dynamicshop-test-image-content".getBytes(StandardCharsets.UTF_8);

        String url = service.upload("products/demo.txt", content, "text/plain");

        assertThat(url).isEqualTo(minio.getS3URL() + "/" + BUCKET + "/products/demo.txt");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("dynamicshop-test-image-content");
    }

    @Test
    void publicUrlOf_khong_can_upload() {
        S3ImageStorageService service = newService();
        assertThat(service.publicUrlOf("products/x.jpg"))
                .isEqualTo(minio.getS3URL() + "/" + BUCKET + "/products/x.jpg");
    }
}
