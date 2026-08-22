# Multi-stage: build bằng Gradle wrapper, chạy bằng JRE gọn hơn — image production, không phải
# image cho local dev (local dev vẫn dùng ./gradlew bootRun --args='--spring.profiles.active=local'
# như hiện tại, xem progress.md). Dùng ở infra/docker/docker-compose.prod.yml.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --version --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
# wget cho HEALTHCHECK/docker-compose healthcheck ở docker-compose.prod.yml — image JRE gốc
# không có sẵn curl/wget.
RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
# app.storage.mode=local (mặc định) ghi vào ./data/images tương đối theo WORKDIR — /app thuộc
# root sau COPY nên appuser không tạo thư mục con được nếu không chown trước.
RUN mkdir -p /app/data/images && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
