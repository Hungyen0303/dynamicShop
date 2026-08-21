# Chạy backend local

## 1. Postgres qua Docker

```bash
cd ../infra/docker
cp .env.example .env        # sửa mật khẩu
docker compose up -d
docker compose ps           # postgres phải healthy
```

Kiểm tra hai role đã tạo:
```bash
docker compose exec postgres psql -U postgres -d dynamicshop -c "\du"
# phải thấy app_user (thường) và app_admin (BYPASSRLS)
```

## 2. Cấu hình

`src/main/resources/application-local.yml` (không commit — có trong `.gitignore`):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dynamicshop
    username: app_user
    password: <APP_USER_PASSWORD trong .env>
  jpa:
    hibernate:
      ddl-auto: validate      # KHÔNG BAO GIỜ update
  flyway:
    enabled: true
```

DataSource thứ hai cho admin dùng `app_admin`.

## 3. Chạy

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew test          # gồm test cô lập tenant (Testcontainers)
```

## Cần hỏi người nếu chưa có
- Firebase service account JSON (cho FCM, sprint 4)
- Cổng 5432 có bị chiếm không
