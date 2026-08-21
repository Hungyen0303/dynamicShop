# INIT.md — Khởi tạo dự án

**Dành cho AI agent.** Hướng dẫn dựng dự án từ skeleton này.

---

## ⚠️ Hai luật của file này

### 1. Thiếu thông tin gì thì HỎI NGƯỜI, đừng đoán

Skeleton cố ý để trống nhiều thứ chỉ chủ dự án mới quyết được. Gặp chỗ trống:
1. **Dừng bước đang làm**
2. Hỏi một câu rõ ràng, **kèm phương án mặc định bạn đề xuất**
3. Chờ trả lời rồi mới tiếp tục

Câu hỏi tốt: *"Bước này cần biết dùng Java 21 được không. Em đề xuất Java 21 + Gradle Kotlin DSL. Anh xác nhận giúp em?"*

Dùng lệnh `/ask-owner` để gom câu hỏi thành một lượt.

### 2. Chỉ làm việc thuộc STAGE HIỆN TẠI

Đọc **`docs/70-stages.md`** trước. Rất nhiều thứ trong skeleton này (CI/CD, in bill, seed, melos, merchant app, admin web) **cố ý chưa làm**.

Thấy thư mục `merchant_app/` trống không có nghĩa là bạn nên dựng nó.

---

## 🟢 STAGE 0 — chỉ làm những thứ dưới đây

**Mục tiêu:** flow chạy thông suốt trên **local** với **2 mock shop**, không cần internet.

Bar hoàn thành — 8 bước này phải chạy được:

```
1. Mở customer_app, chọn mock shop A
2. Storefront hiện đúng theme + menu của shop A
3. Đổi sang mock shop B → theme và menu KHÁC HẲN
4. Thêm món vào giỏ, đặt hàng
5. Đơn xuất hiện trong DB, order_events có dòng PENDING
6. Gọi API xác nhận đơn (curl) → trạng thái đổi, order_events thêm dòng
7. Gửi lại đúng request đặt hàng đó → KHÔNG tạo đơn thứ hai
8. Query DB với tenant B → KHÔNG thấy đơn của tenant A
```

Bước 3, 7, 8 quan trọng nhất — chúng chứng minh SDUI, idempotency và multi-tenant thật sự hoạt động.

### Thứ tự — không đảo

```
1. Root (git)
2. Docker Postgres          ← đã có sẵn compose file
3. Contracts (tối thiểu)
4. Backend                  ← phần lớn công sức Stage 0
5. customer_app
6. Mock data 2 shop
```

---

## 1. Root

```bash
git init && git branch -M main
```

**Chiến lược git:** monorepo. Xem **`docs/80-git-workflow.md`** — có sẵn câu trả lời cho lo ngại "sửa mobile thì BE có thấy không" (dùng `git sparse-checkout`).

Có sẵn trong skeleton: `.gitignore`, `.gitattributes`, `AGENTS.md`.

Commit đầu: `chore: khởi tạo skeleton dự án`

**Chưa cần ở Stage 0:** git hooks, CI, branch protection.

**Không commit:** `.env`, `google-services.json`, keystore, `application-local.yml`, `**/generated/`.

---

## 2. Docker Postgres

Đã có sẵn `infra/docker/docker-compose.yml`.

```bash
cd infra/docker
cp .env.example .env        # sửa mật khẩu
docker compose up -d
docker compose ps           # phải healthy
```

Xác nhận hai role:
```bash
docker compose exec postgres psql -U postgres -d dynamicshop -c "\du"
# phải thấy: app_user (thường), app_admin (BYPASSRLS)
```

**Không đổi cấu trúc role này** — kiến trúc multi-tenant dựa vào nó.

**Hỏi người:** cổng 5432 có bị chiếm không?

---

## 3. Contracts — tối thiểu cho Stage 0

Đã có: `tokens.json`, `blocks.registry.json`, `order-states.json`, `metrics.json`.

Stage 0 chỉ cần thêm:
- `storefront.schema.json` — cấu trúc JSON storefront

**Chưa cần:** `openapi.yaml` đầy đủ, generator, `make verify-contracts`. Ở Stage 0, viết model bằng tay nhanh hơn. Dựng generator ở Stage 1 khi API đã ổn định.

**Hỏi người** trước khi chọn công cụ sinh code — đừng tự quyết.

---

## 4. Backend — phần lớn công sức Stage 0

```bash
cd backend   # Spring Initializr hoặc gradle init
```

**Hỏi người:** Java 21? Gradle Kotlin DSL hay Groovy? Phiên bản Spring Boot?

**Dependency tối thiểu:** Web, Data JPA, Security, Validation, PostgreSQL Driver, Flyway, Testcontainers.
Thêm gì ngoài danh sách ⇒ hỏi.

**Cấu trúc package đã có sẵn** trong `src/main/java/vn/dynamicshop/` — giữ nguyên tên.

### Thứ tự bắt buộc

```
1. application.yml + kết nối Docker Postgres
2. Flyway V1__init.sql: bảng lõi + RLS + policy
3. TenantContext + filter + SET LOCAL
4. 🔴 TEST CÔ LẬP TENANT (Testcontainers)   ← phải xanh trước khi làm tiếp
5. Auth đơn giản (JWT + mật khẩu fixture, KHÔNG OTP/Zalo)
6. Catalog + storefront API
7. Order + state machine + order_events
8. Idempotency
9. Outbox (bảng + worker ghi log, CHƯA nối FCM)
```

🔴 **Không viết endpoint nào trước khi bước 4 xanh.** Đây là nền móng — sai thì phải đập hết.

`application.yml`: `spring.jpa.hibernate.ddl-auto: validate`. **Không bao giờ `update`.**

**Ảnh ở Stage 0:** lưu thư mục local, Spring serve tĩnh. Không R2, không CDN.

---

## 5. customer_app

```bash
flutter create --org vn.dynamicshop --platforms=android customer_app
```

**Hỏi người:** phiên bản Flutter? Dùng FVM không?

### Stage 0 làm gọn

- **Chỉ 3 flavor môi trường:** `dev`, `staging`, `prod`. Flavor theo shop là Stage 2+, xem `docs/10-customer-app.md`.
- **Chưa dùng melos.** Dùng `path:` dependency trực tiếp trong `pubspec.yaml`:
  ```yaml
  dependencies:
    ds_tokens: { path: ../../packages/ds_tokens }
    ds_blocks: { path: ../../packages/ds_blocks }
  ```
- Trỏ về `http://10.0.2.2:8080` (emulator Android nhìn thấy localhost của máy host)
- Thêm màn hình dev cho phép **đổi giữa mock shop A và B** — cần cho bước 3 của bar hoàn thành
- `assets/default_storefront.json` — fallback tầng 3

**Chưa cần:** Firebase, deep link, Zalo, lint CI, golden test đầy đủ (2–3 cái tiêu biểu là đủ).

---

## 6. Mock data 2 shop

Flyway migration chỉ chạy ở profile `local`:

```
V900__mock_tenant_a.sql    quán bún — theme đỏ, 3 danh mục, 12 món
V901__mock_tenant_b.sql    quán trà sữa — theme xanh, 2 danh mục, 8 món
```

⚠️ Hai shop phải **khác nhau rõ rệt về theme và layout**. Nếu giống nhau, bạn không chứng minh được gì.

Đặt từ V900 để không đụng dãy migration thật.

---

## ✅ Stage 0 xong khi

- [ ] `docker compose up -d` chạy, hai role tồn tại
- [ ] `./gradlew bootRun` kết nối được Postgres
- [ ] **Test cô lập tenant xanh, dùng Testcontainers không phải H2**
- [ ] `ddl-auto: validate` ở mọi profile
- [ ] customer_app build và chạy trên emulator
- [ ] **Cả 8 bước của bar hoàn thành đều pass**
- [ ] Bạn giải thích được toàn bộ flow cho người khác nghe

Chỉ khi tất cả xanh mới sang Stage 1.

---

## 🔴 Stage 1+ — chưa làm, để tham khảo

<details>
<summary>Stage 1 — Hạ tầng ảo + Firebase</summary>

VPS + Docker Compose, domain + Caddy TLS, Firebase (FCM, Crashlytics), nối outbox worker vào FCM, R2 cho ảnh + resize lúc upload, customer_app trỏ server thật.

**Hỏi người:** VPS nhà cung cấp nào? Tên miền? Firebase project — dev/prod riêng hay chung?
</details>

<details>
<summary>Stage 2 — Merchant app</summary>

Android only. Nhận đơn + FCM + foreground service + chuông. Xử lý OEM giết app nền. Offline queue. **Chụp ảnh/quay video và đăng trực tiếp từ điện thoại.** Báo cáo + push tổng kết cuối ngày. Nút "Đã nhận tiền".

Chưa làm: in bill nhiệt (Stage 5).

**Hỏi người:** có máy Xiaomi/Oppo thật để test không?
</details>

<details>
<summary>Stage 3 — Vận hành</summary>

Next.js admin tối giản, quota dung lượng, export dữ liệu cho shop, CI/CD, backup + diễn tập restore.
</details>

<details>
<summary>Stage 4–5</summary>

Seed script, melos, studio_web (có thể không bao giờ), CRM. Rồi in bill nhiệt, Ahamove, đối soát tự động.
</details>

**Zalo — hoãn vô thời hạn.** Không tự ý thêm.

---

## 📋 Cần hỏi người — Stage 0

Gom hỏi một lượt:

1. Cổng 5432 có bị chiếm không?
2. Java 21 được chứ? Gradle Kotlin DSL hay Groovy?
3. Phiên bản Spring Boot 3.x cụ thể?
4. Phiên bản Flutter? Dùng FVM không?
5. Test trên emulator hay máy Android thật?
6. Hai mock shop muốn đặt tên gì? (mặc định: quán bún + quán trà sữa)
7. Repo đặt ở GitHub hay đâu?

**Chưa cần hỏi ở Stage 0:** Firebase, domain, VPS, máy in, tài khoản store, Zalo, công cụ sinh code.

---

## Nếu bế tắc

- **Task thuộc stage sau** → nói rõ nó ở stage nào, hỏi người có muốn làm sớm không
- **Mâu thuẫn giữa doc và yêu cầu** → hỏi, đừng tự chọn bên nào
- **Cần thêm dependency** → hỏi
- **Một bất biến cản đường** → hỏi. Bất biến có lý do; nếu thật sự sai thì sửa doc trước, code sau.
- **Không chạy được lệnh** → nói rõ, đừng bỏ qua im lặng rồi báo "đã xong"


Đây là thông tin github, hãy thêm vào project này cho tôi 
echo "# dynamicShop" >> README.md
git init
git add README.md
git commit -m "first commit"
git branch -M main
git remote add origin https://github.com/Hungyen0303/dynamicShop.git
git push -u origin main