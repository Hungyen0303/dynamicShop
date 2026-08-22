plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "vn.dynamicshop"
version = "0.0.1-SNAPSHOT"
description = "DynamicShop backend"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")

	// Stage 1 — object storage cho ảnh (R2/MinIO, cùng giao thức S3). Chưa managed bởi
	// Spring Boot BOM nên khai version tường minh. Stage 1 được chủ dự án duyệt trực tiếp
	// 2026-08-22 (xem progress.md, đoạn "Cập nhật 2026-08-22 sau khi đóng Stage 0"), không
	// phải agent tự suy diễn. software.amazon.awssdk:s3 kéo theo netty-nio-client mặc định.
	implementation("software.amazon.awssdk:s3:2.54.1")

	// Stage 1 — FCM (duyệt cùng lúc với dependency S3 ở trên, xem progress.md). Thuần JVM
	// (không native), an toàn build. Fallback log-only khi chưa cấu hình Firebase thật —
	// xem notification/FirebaseFcmConfig.
	implementation("com.google.firebase:firebase-admin:9.10.0")

	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	// MinIO — CHỈ dùng trong test (round-trip S3ImageStorageService), KHÔNG thêm vào
	// infra/docker/docker-compose.yml (giữ nguyên workflow dev hiện có).
	testImplementation("org.testcontainers:testcontainers-minio")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
