# INIT — backend

🟢 **Stage 0 — đây là phần ưu tiên cao nhất.**

Đọc `../INIT.md` mục 4 và `../docs/70-stages.md` trước. Luật: **thiếu gì thì hỏi người, đừng đoán.**

## Phụ thuộc
Phải init xong: root (git), infra (Docker Postgres), contracts.

## Cần hỏi người trước khi bắt đầu
1. Java 21 được chứ? Gradle Kotlin DSL hay Groovy?
2. Phiên bản Spring Boot 3.x cụ thể?
3. Hai mock shop đặt tên gì? (mặc định: quán bún + quán trà sữa)

**Chưa cần hỏi ở Stage 0:** Firebase, công cụ sinh code, VPS.

## Dependency tối thiểu
Web, Data JPA, Security, Validation, PostgreSQL Driver, Flyway, Actuator, Testcontainers.

Thêm gì ngoài danh sách ⇒ hỏi.

## Cấu trúc package đã có sẵn
Giữ nguyên tên trong `src/main/java/vn/dynamicshop/`. Đừng đổi.

## Xong khi (Stage 0)
- [ ] `./gradlew bootRun` chạy được, kết nối Docker Postgres
- [ ] Flyway V1 áp dụng được, bảng lõi có RLS
- [ ] **Test cô lập tenant xanh, dùng Testcontainers không phải H2**
- [ ] `ddl-auto: validate` trong mọi profile
- [ ] `application-local.yml` nằm trong `.gitignore`
- [ ] Mock data 2 shop nạp được (V900, V901, chỉ profile `local`)
- [ ] Storefront API trả về layout + data trong MỘT response
- [ ] Idempotency hoạt động: gửi 2 lần cùng key → 1 đơn
- [ ] Outbox có bảng + worker (ghi log, chưa nối FCM)
