# progress.md — Stage 0 ĐÃ ĐÓNG, Stage 1 ĐANG LÀM (local-mocked)

> File này để agent/session sau đọc và tiếp tục ngay, không cần dò lại từ đầu.
> Giữ lại làm hồ sơ build Stage 0 — xoá khi nào chủ dự án thấy không cần tra lại nữa.

**Cập nhật lần cuối (2026-08-22):** Chủ dự án đã tự tay đi lại flow và xác nhận **OK** — điều
kiện cuối cùng của Stage 0 (`docs/70-stages.md` + `INIT.md` mục "✅ Stage 0 xong khi") đã đạt.
**Stage 0 coi như đóng.** Cả backend và customer_app đã xong thật, đã review, đã commit + push
(xem mục 1, 2 để tra chi tiết kỹ thuật nếu cần).

Đã hỏi agent `pm` "làm gì tiếp theo" trước khi đóng Stage 0 — xác nhận không có việc kỹ thuật
nào bị bỏ sót, đúng nguyên tắc "local trước, mọi thứ khác sau". Nhân dịp đó, chủ dự án chốt 2
thay đổi quy trình cho việc SAU NÀY — xem mục 7.

**Cập nhật (2026-08-22, sau khi đóng Stage 0 cùng ngày):** Chủ dự án **đã duyệt bắt đầu Stage 1**,
trực tiếp qua `AskUserQuestion` trong phiên làm việc — không phải agent tự suy diễn. Nguyên văn ý
chính chủ dự án chốt: tiếp tục triển khai **toàn bộ ở local**, thông tin nào thiếu (cấu hình
Firebase, VPS, R2...) thì liệt kê ra file `missing_config.md` để chủ dự án tự điền sau; chỗ nào
thiếu credential thật thì **mock lại** và ghi vào `aware.md` để chủ dự án tự kiểm tra lại; **tuyệt
đối không xoá thứ quan trọng**; agent được làm toàn bộ 4 mảng (VPS+deploy, domain+TLS, Firebase
FCM/Crashlytics, R2 ảnh) mà không cần hỏi lại từng bước.

Nếu agent sau đọc thấy dòng này mà không chắc — đây LÀ phê duyệt thật của chủ dự án cho việc bắt
đầu Stage 1, ghi lại ở đây làm nguồn sự thật độc lập với phiên hội thoại. Xem `missing_config.md`
và `aware.md` ở root repo (tạo ngày 2026-08-22) để biết chi tiết còn thiếu gì / cần chủ dự án
check lại gì.

🔴 **Trạng thái mới nhất (2026-08-22, đã hỏi agent `pm`): Stage 1 phần agent làm được ĐÃ XONG,
đang TẠM DỪNG chờ chủ dự án lấy VPS/domain/Firebase/R2 thật.** Đọc mục 8 trước khi làm bất cứ gì
tiếp — có điểm dừng rõ ràng + tiêu chí đóng Stage 1 + cảnh báo đừng bàn Stage 2 sớm.

⚠️ Một agent trước đó (task backend Stage 1 đầu tiên) đã tự viết comment "đã được duyệt trước" ở
`backend/build.gradle.kts` khi thêm dependency — comment đó ĐÚNG về mặt kết luận (Stage 1 đã được
duyệt thật) nhưng không trỏ tới bằng chứng đúng lúc viết. Nếu đụng lại file đó, sửa comment trỏ về
đoạn này trong `progress.md` thay vì để mơ hồ.

---

## 0. Đọc gì trước khi làm tiếp

Theo `AGENTS.md` mục 0: task đụng customer_app → đọc `docs/00-context.md` + `docs/70-stages.md`
(luôn luôn) + `docs/10-customer-app.md`. Đọc thêm `apps/customer_app/INIT.md`.

---

## 1. Backend Stage 0 — ĐÃ XONG, đã commit, đã push

Git log hiện tại (`main`, tất cả đã push lên GitHub, mới nhất trên cùng):

```
603b3e8  doc: cập nhật progress.md — Stage 0 kỹ thuật đã xong (backend + customer_app)
cb58976  mo: dựng customer_app Stage 0 — SDUI, 3 tầng fallback, giỏ hàng/checkout hardcode
f1e224a  doc: cập nhật progress.md — backend Stage 0 xong, tiếp theo là customer_app
3d48279  be: sửa GUC tenant dùng doBegin() thay AOP pointcut — repository gọi trực tiếp
         không qua service từng bị mất bảo vệ RLS
0357c45  be: hoàn thành backend Stage 0 — tenant isolation, auth, storefront, order,
         idempotency, outbox
8cd1637  ct: thêm contracts/storefront.schema.json
bed7b8f  be: khởi tạo Gradle project (Spring Boot 4.1.1, Java 21)
1c23c54  chore: khởi tạo skeleton dự án
```

### Hai bug thật đã tìm ra và sửa trong lúc review — đáng đọc nếu đụng lại `common/tenant/`

Khi review commit `0357c45`, phát hiện bộ test cô lập tenant dùng role superuser của
Testcontainers cho mọi việc → RLS chưa từng được kiểm chứng thật (superuser luôn bypass RLS
bất kể `FORCE`). Sửa bằng cách tạo role `app_user`/`app_admin` thật (không superuser) trong
container test — nhưng việc này lại lộ ra **hai bug kiến trúc thật, không phải bug của bản sửa**:

1. **`TenantGucInterceptor` (AOP pointcut) không bao giờ chạy cho lời gọi repository trực
   tiếp.** Spring Data JPA tự dựng proxy cho repository qua `RepositoryFactorySupport`, không đi
   qua `InfrastructureAdvisorAutoProxyCreator` mà advisor tuỳ biến bám vào. Production code luôn
   gọi repository qua một service `@Transactional` (proxy chuẩn, advisor bắt đúng) nên không lộ,
   nhưng bất kỳ code nào sau này gọi thẳng repository sẽ mất bảo vệ GUC/RLS trong im lặng.
   **Đã sửa tận gốc**: xoá `TenantGucInterceptor`/`TenantGucAdvisorConfig`/`TransactionOrderConfig`,
   thay bằng `TenantAwareJpaTransactionManager` (`common/tenant/`) — override
   `JpaTransactionManager.doBegin()`, chạy cho MỌI transaction vật lý mới bất kể ai khởi tạo, vì
   toàn app chỉ dùng chung một `PlatformTransactionManager` (đăng ký ở `HibernateTenantConfig`).
2. **`JdbcTemplate` bean mơ hồ.** `AdminDataSourceConfig` định nghĩa bean `adminJdbcTemplate`
   (kiểu `JdbcTemplate`) → `@ConditionalOnMissingBean(JdbcTemplate.class)` của Boot check THEO
   TYPE nên không tự tạo `JdbcTemplate` mặc định cho `app_user` nữa → bất kỳ `@Autowired
   JdbcTemplate` không kèm `@Qualifier` sẽ vô tình lấy nhầm bean admin (BYPASSRLS). Đã kiểm tra:
   không ảnh hưởng production (không có chỗ nào autowire kiểu này trong `src/main` lúc đó).
   **Đã sửa**: khai báo tường minh `@Primary JdbcTemplate` cho `app_user` trong
   `PrimaryDataSourceConfig`.

Nếu sau này thêm code chạm tenant/RLS/DataSource, đọc kỹ 2 file này trước:
`TenantAwareJpaTransactionManager.java` và `PrimaryDataSourceConfig.java` — cả hai đều có
Javadoc giải thích đầy đủ lý do.

### Kết quả cuối cùng
- `./gradlew clean test` — **24/24 xanh** (đếm trực tiếp từ XML report, không phải cache).
- `./gradlew bootRun --args='--spring.profiles.active=local'` + curl smoke test qua HTTP thật:
  storefront 2 shop theme/menu khác hẳn nhau, idempotency đúng (2 lần cùng key → 1 đơn),
  merchant A login + transition đơn của mình → 200, merchant B thử transition đơn tenant A →
  404 `ORDER_NOT_FOUND` (đúng, không lộ dữ liệu).

### Những gì backend Stage 0 đã dựng xong
- `contracts/storefront.schema.json`
- DataSource split 3 kết nối: `spring.datasource` (`app_user`, RLS), `spring.flyway`
  (`postgres`, chỉ migrate), `app.admin-datasource` (`app_admin`, BYPASSRLS, chỉ trong `admin/`)
- `V1__init.sql` — toàn bộ bảng lõi + RLS + FORCE + policy + index đúng `docs/31-database.md`
- `TenantAwareJpaTransactionManager` — phát `SET LOCAL` qua `set_config(..., true)` tham số hoá,
  chạy ở `doBegin()` của transaction manager duy nhất trong app
- Auth JWT đơn giản, 2 mặt phẳng, route merchant login có slug (`POST
  /v1/merchant/{slug}/auth/login`) — đã duyệt, xem mục "Quyết định đã duyệt"
- Storefront API một response, DTO riêng (`StorefrontResponseDto`), không lộ entity
- `OrderStateMachine` — điểm vào duy nhất, có test đối chiếu trực tiếp với
  `contracts/order-states.json` (`OrderStateMachineContractTest`) để chống drift
- Idempotency đúng spec (200/201 lặp lại khi trùng hash, 409 khi khác hash, 400 khi thiếu header)
- Outbox bảng + worker ghi log
- Test cô lập tenant (Testcontainers, role `app_user`/`app_admin` THẬT không phải superuser) —
  bao gồm test native-SQL độc lập Hibernate (`rls_tu_no_chan_native_query_khong_qua_hibernate`)
  và drift-guard chống role superuser lọt lại (`datasource_chinh_khong_phai_superuser_va_khong_bypassrls`)

### Môi trường local đang chạy
- Docker container `ds-postgres` (postgres:16-alpine), cổng **5433** (không phải 5432 — máy có
  Postgres khác chiếm cổng đó), healthy.
- 3 role trong DB thật: `postgres`/`ds_local_postgres_dev` (superuser, chỉ Flyway dùng),
  `app_user`/`ds_local_app_user_dev` (RLS), `app_admin`/`ds_local_app_admin_dev` (BYPASSRLS).
- `infra/docker/.env` và `backend/src/main/resources/application-local.yml` đã có sẵn, đúng
  gitignore, không cần tạo lại.
- Mock data: tenant A slug `bun-co-ba` (merchant `0901000001` / `bunca123`), tenant B slug
  `tra-sua-ngoc` (merchant `0902000001` / `trasua123`).

---

## 2. customer_app — ĐÃ XONG, đã commit, đã push

Commit `cb58976 mo: dựng customer_app Stage 0 — SDUI, 3 tầng fallback, giỏ hàng/checkout
hardcode` — đã review (không chỉ tin báo cáo): đọc lại `StorefrontRenderer`/`BlockRegistry`
(hai chốt chặn forward-compat + error-boundary đúng nguyên văn docs), `StyleResolver` (đúng
chuỗi `blockOverride ?? variantPreset ?? tenantTheme ?? appDefault`), `OrderRepository` (giữ
nguyên `Idempotency-Key` qua `KvStore` khi retry, chỉ xoá key sau khi đơn tạo thành công). Tự
chạy lại `flutter test` ở cả 6 nơi (`ds_core`, `ds_tokens`, `ds_sdui`, `ds_api`, `ds_blocks`,
`customer_app`) — **35/35 xanh**, không phải số báo cáo suông.

Đã dựng: 6 package (`ds_core`, `ds_tokens`, `ds_components`, `ds_blocks`, `ds_sdui`, `ds_api`)
+ `apps/customer_app` với 3 flavor môi trường, màn dev đổi shop A/B, fallback 3 tầng (server →
cache đĩa theo slug → `assets/default_storefront.json`), giỏ hàng/checkout/theo dõi đơn hardcode
(không SDUI), 8 block SDUI đủ theo registry.

Đã verify TAY trên máy Android thật (Samsung SM-A256E, qua `adb reverse tcp:8080 tcp:8080`):
mở app thấy đúng theme đỏ shop A, đổi sang shop B thấy theme xanh khác hẳn, đặt đơn thật — query
`docker exec ds-postgres psql` xác nhận `orders` có dòng đúng tenant, `order_events` có PENDING
cùng transaction, tắt backend thì app rơi xuống bundled asset chứ không trắng màn hình.

### Còn một mục chưa tự verify qua UI thật (biết trước, chấp nhận được)
Gửi lại đúng `Idempotency-Key` qua UI thật (double-tap "Đặt hàng") — nút bị disable lúc đang
gửi nên không mô phỏng tay được. Đã xác nhận đúng bằng đọc code (`OrderRepository.submit`) +
test phía backend (`IdempotencyOrderTest`, đã xanh). Nếu muốn chắc chắn 100%, có thể viết một
integration test gọi `apiClient.createOrder` hai lần liên tiếp cùng key giả lập — chưa làm, để
tuỳ chọn cho ai cần.

### Bước tiếp theo — ĐÃ XONG
Chủ dự án đã tự tay đi lại đủ 8 bước bar hoàn thành trong `docs/70-stages.md` và xác nhận OK
(2026-08-22). Stage 0 đóng — xem đầu file.

---

## 3. Quyết định đã duyệt (khỏi hỏi lại)

1. Route login merchant có slug (`POST /v1/merchant/{slug}/auth/login`) — chấp nhận, giải quyết
   vòng lặp gà-trứng hợp lý.
2. JWT dev secret commit thẳng trong `application.yml` — chấp nhận cho Stage 0 local-only, phải
   đổi trước Stage 1.
3. Outbox worker duyệt từng tenant (không BYPASSRLS) — chấp nhận, đúng tinh thần "admin
   datasource chỉ dùng trong package admin/".
4. Không rate-limit (Bucket4j) trên storefront API — đúng yêu cầu, không phải dependency được
   duyệt, để Stage sau.
5. Idempotency không khoá phân tán chống request trùng key gửi đồng thời — chấp nhận ở quy mô
   Stage 0.
6. Spring Boot **4.1.1** (không phải 3.x — Initializr không còn bản 3.x khi làm) — người dùng đã
   duyệt qua `AskUserQuestion`.
7. GUC tenant set qua `TenantAwareJpaTransactionManager.doBegin()` thay vì AOP pointcut hoặc
   `org.hibernate.Interceptor` thô — người dùng đã duyệt hướng "sửa tận gốc" qua
   `AskUserQuestion`.
8. Flutter **3.43.0 kênh BETA** (không FVM, không bản stable riêng) — người dùng đã duyệt qua
   `AskUserQuestion`.
9. Test customer_app trên **máy Android thật** (Samsung SM-A256E) thay vì emulator — người dùng
   đã duyệt qua `AskUserQuestion`. Kết nối qua `adb reverse tcp:8080 tcp:8080` (không phải
   `10.0.2.2` — địa chỉ đó chỉ dùng được cho emulator).
10. Cache storefront tầng 2 key theo `slug` thay vì `tenant_id` (khách chưa đăng nhập, không có
    UUID tenant ở phía client) — chấp nhận, hợp lý cho Stage 0.
11. Font: bundle thật Be Vietnam Pro + Inter, font khác trong `allowed_fonts` tải qua
    `google_fonts` (đã pre-duyệt dependency này khi giao việc) với fallback nuốt lỗi mạng —
    chấp nhận.

12. **Vòng đời JWT merchant: TTL dài + tự đăng nhập lại** (KHÔNG dùng refresh token) — chủ dự án
    đã duyệt qua `AskUserQuestion` (2026-08-22). Access token sống lâu, app merchant lưu số điện
    thoại + mật khẩu an toàn trên máy, hết hạn thì tự gọi lại login. Lý do chọn: ít code, ít bảng,
    hợp quy mô hiện tại. Đánh đổi đã biết và chấp nhận: mật khẩu nằm trên máy, thu hồi phiên khó
    hơn. Đây là câu trả lời cho rủi ro số 1 mà PM nêu ở mục 9 — đừng tự dựng bảng refresh token.
13. **Thumbnailator** cho resize ảnh phía server (sprint 2.3) — chủ dự án đã duyệt dependency này
    qua `AskUserQuestion` (2026-08-22). Java thuần, không native lib, xử lý xoay EXIF sẵn. Không
    dùng ImageIO thuần (phải tự viết scale chất lượng + tự xử lý EXIF, dễ ra ảnh vỡ).

## 4. KHÔNG được tự ý làm (nhắc lại từ AGENTS.md, phòng agent sau quên)

⚠️ Danh sách này viết lúc còn ở Stage 0 — dòng "Không làm gì thuộc Stage 1+ (FCM thật, VPS...)"
**đã lỗi thời** kể từ khi chủ dự án duyệt Stage 1 (xem đầu file + mục 6). Giữ nguyên các mục còn
lại vì vẫn đúng.

- Không thêm dependency ngoài phạm vi đã duyệt: backend Stage 0 (web/webmvc, data-jpa, security,
  validation, postgresql, flyway, actuator, testcontainers) + Stage 1 đã duyệt (AWS SDK S3,
  firebase-admin, testcontainers-minio — xem mục 6). Với customer_app: xem `INIT.md` mục 5, hỏi
  trước khi thêm gì ngoài Flutter SDK + `ds_*` packages nội bộ (Firebase phía client CHƯA thêm —
  xem `aware.md`).
- ~~Không làm gì thuộc Stage 1+~~ — đã duyệt Stage 1 (VPS/deploy, Firebase FCM, R2), xem mục 6.
  merchant_app, admin web, Zalo vẫn CHƯA được làm (Stage 2/3, chưa bàn tới).
- Không `git push --force`, không amend commit đã có.
- Không tắt/`@Disabled` test để né lỗi — thấy đỏ thì sửa nguyên nhân.

---

## 6. Stage 1 — backend, local-mocked (2026-08-22, uncommitted)

Chưa commit — mọi file dưới đây vẫn nằm trong working tree, `git status` cho thấy đủ. Test
`cd backend && ./gradlew clean test` → **29/29 xanh**. Đã test THẬT hạ tầng deploy ở local (không
phải chỉ code review) — xem chi tiết mục "Đã test thật" bên dưới.

### Đã dựng
- **Object storage adapter** (`backend/src/main/java/vn/dynamicshop/common/storage/`) —
  `ImageStorageService` interface, `LocalDiskImageStorageService` (mặc định, hành vi Stage 0 y
  hệt cũ), `S3ImageStorageService` (R2/MinIO qua AWS SDK v2, `app.storage.mode=s3`). Test round
  -trip thật qua Testcontainers MinIO (`S3ImageStorageServiceTest`) — kể cả set bucket policy
  public-read (bug thật gặp lúc build: bucket MinIO mới tạo mặc định private → 403, phải set
  policy, y hệt bước cần làm ở R2 production).
- **FCM adapter** (`backend/src/main/java/vn/dynamicshop/notification/`) — `FcmSender`
  interface, `LogOnlyFcmSender` (fallback mặc định), `FirebaseFcmConfig` + `FirebaseFcmSender`
  (Firebase Admin SDK thật, chỉ kích hoạt khi `app.firebase.enabled=true` VÀ file service-account
  tồn tại — thiếu gì cũng fallback log-only, KHÔNG BAO GIỜ crash app khi thiếu cấu hình). Nối vào
  `OutboxBatchProcessor` — vẫn đúng bất biến #7 (chỉ gọi từ đây, ngoài transaction nghiệp vụ).
  `deviceToken` hiện luôn `null` (chưa có bảng đăng ký device merchant — Stage 2), nên dù bật
  Firebase thật vẫn chưa gửi được push nào — kiến trúc đúng, chỉ chưa có consumer.
- **`docs/90-api-contract.md`** — tài liệu API tay đầu tiên theo quy trình mới (mục 5 dưới đây):
  liệt kê đủ 4 endpoint hiện có (storefront, tạo đơn, merchant login, merchant transition), error
  shape, header bắt buộc. Đây là nguồn FE/mobile đọc từ giờ, không đọc `.java` nữa.
- **Hạ tầng deploy dạng production** — `backend/Dockerfile` (multi-stage, non-root user),
  `infra/docker/docker-compose.prod.yml` (postgres + backend + caddy, project name `ds-prod`
  riêng biệt), `infra/docker/Caddyfile` (reverse proxy + TLS tự động), `infra/docker/
  .env.prod.example`. **Không đụng** `infra/docker/docker-compose.yml` (compose dev hiện có).
- `backend/src/main/resources/application-docker.yml` — profile `docker`, mọi giá trị đọc từ env
  var truyền qua compose, không hardcode gì.

### Đã test THẬT (không phải chỉ đọc code)
- `docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build` chạy thật ở máy
  dev — build image Docker thành công, cả 3 container (`ds-postgres-prod`, `ds-backend-prod`,
  `ds-caddy-prod`) lên khoẻ.
- `curl -k https://localhost:8443/actuator/health` → `200 {"status":"UP"}` — xác nhận chuỗi
  request ngoài → Caddy (TLS qua CA nội bộ vì domain là `localhost`) → backend → postgres chạy
  đúng, trước khi có VPS/domain thật.
- Đã `down` + dọn volume test sau khi xong, dev stack (`ds-postgres`) không bị ảnh hưởng lâu dài.

### 🔴 Sự cố thật lúc test — đã xử lý, không mất dữ liệu (đọc `aware.md` để biết đầy đủ)
Lần đầu chạy `docker-compose.prod.yml`, do cùng thư mục với `docker-compose.yml` (dev) và cùng
service key `postgres`, Docker Compose lấy project name mặc định trùng nhau → coi hai container
Postgres là MỘT service → "recreate" mất container Postgres **dev thật đang chạy**. Volume dữ
liệu (`docker_ds-pgdata`) không mất, phục hồi ngay, verify lại 2 mock tenant còn nguyên qua
`psql`. Đã sửa gốc: ghim `name: ds-prod` ở đầu `docker-compose.prod.yml`.

### customer_app — Firebase Crashlytics (2026-08-22, tiếp sau khi được yêu cầu "continue")
Đã thêm `firebase_core`/`firebase_crashlytics` vào `pubspec.yaml`, `lib/config/crash_reporting.dart`
(bắt lỗi khi chưa có Firebase project thật, không crash app), gọi ở cả 3 `main_*.dart`. Plugin
Gradle (`android/settings.gradle.kts`, `android/app/build.gradle.kts`) chỉ tự apply khi tìm thấy
`google-services.json` thật — hiện chưa có file nào nên build Android không đổi. **Verify bằng
build thật** (không chỉ đọc code, vì đây là thay đổi Gradle): `flutter pub get` OK, `flutter
analyze` sạch, `flutter build apk --flavor dev -t lib/main_dev.dart --debug` build thành công. Đây
là compile-check tối thiểu, KHÔNG phải `flutter test`/verify thiết bị — xem `aware.md` nếu chủ dự
án thấy việc build này vượt phạm vi "chỉ dựng code" đã chốt ở mục 7. Chưa build thử flavor
`staging`/`prod` (guard logic giống hệt) và chưa đụng iOS (máy dev Windows).

### Còn thiếu (không chặn local, chỉ chặn lên thật)
Xem `missing_config.md` — VPS, domain, Firebase service-account thật (backend), R2 credential
thật, backup tự động, và Firebase project thật cho `customer_app` (`google-services.json`/
`GoogleService-Info.plist` per flavor — code đã sẵn sàng, chỉ chờ file).

### Bước tiếp theo
Chưa commit gì trong mục này. Khi chủ dự án xem qua `missing_config.md`/`aware.md` và OK, có thể
commit theo từng phần (backend storage/FCM, docs API, hạ tầng deploy) — hoặc gom một commit, tuỳ
chủ dự án quyết, agent không tự commit khi chưa được yêu cầu. Xem thêm mục 8 — đã hỏi PM, khuyến
nghị commit trước khi tạm dừng chờ credential thật.

---

## 8. 🔴 Đã hỏi agent `pm` "làm gì tiếp theo" (2026-08-22) — ĐIỂM DỪNG, đọc trước khi làm tiếp Stage 1

Hỏi ngay sau khi Stage 1 local-mocked xong (mục 6). Kết luận PM, tóm tắt để agent sau khỏi hỏi
lại — chi tiết đầy đủ nằm trong lịch sử hội thoại phiên này, đây là bản ghi bền vững:

**QUYẾT ĐỊNH: DỪNG AGENT.** Không còn việc kỹ thuật nào của Stage 1 mà agent tự làm tiếp được —
cả 5 mảng Stage 1 (VPS+deploy, domain+TLS, Firebase FCM, R2, Crashlytics client) đã mock/test đủ
ở mức có thể làm mà không cần tài khoản thật. Phần còn lại toàn bộ là **chủ dự án tự đi lấy**: VPS,
domain, Firebase project, Cloudflare R2 — xem checklist đầy đủ + "tiêu chí đóng Stage 1" ở đầu
`missing_config.md`.

1. **Việc cho agent lúc này:** không có, trừ khi chủ dự án đã điền được ít nhất một mục trong
   `missing_config.md` — lúc đó quay lại đúng mục đó (mỗi mục đều có ghi "→ agent làm gì tiếp").
2. **Stage 2 (merchant_app): CHƯA bàn, kể cả để lên kế hoạch.** `docs/70-stages.md`: *"Stage 2
   bắt đầu khi Stage 1 xong, FCM gửi/nhận ổn định."* FCM mới log-only/fallback, chưa gửi push
   thật — bất kỳ thiết kế merchant_app nào dựa trên FCM lúc này là đoán trên giả định chưa kiểm
   chứng. Agent sau thấy yêu cầu đụng `merchant_app`: dừng lại, nhắc lại đúng câu này, hỏi người.
3. **"resize ảnh lúc upload"** (có nhắc trong mô tả Stage 1 ở `docs/70-stages.md`) — CỐ Ý chưa
   làm, không phải thiếu sót: chưa có endpoint HTTP upload ảnh nào (chụp ảnh món ăn là
   `merchant_app`, Stage 2), dựng resize bây giờ là đoán kích thước/định dạng mà Stage 2 chưa xác
   nhận — đúng nguyên tắc `docs/40-pm.md` (làm sớm cái chưa ai cần = maintain trước khi biết đúng
   hay sai). Gộp việc này vào khi Stage 2 thật sự cần.
4. **Việc "chưa commit" — PM đánh giá là rủi ro vận hành đáng ưu tiên trước khi tạm dừng**: toàn
   bộ khối lượng Stage 1 (mục 6) vẫn chỉ nằm trong working tree, chưa có git history bảo vệ, và đã
   có một sự cố thật (suýt mất container Postgres dev — xem mục 6) cho thấy máy local không phải
   không có rủi ro thao tác nhầm. Khuyến nghị: commit **trước khi** để máy trống đi lấy
   VPS/domain/Firebase/R2 trong vài ngày/tuần tới.

**Tiêu chí đóng Stage 1 thật** (chuyển từ `missing_config.md`, để khỏi phải mở hai file): deploy
`docker-compose.prod.yml` lên VPS thật + domain thật + TLS Let's Encrypt thật (không phải CA nội
bộ) → `customer_app` build flavor trỏ domain thật, chạy trên máy Android thật qua **mạng 4G**
(không wifi/localhost), đặt đơn thành công, verify DB trên VPS → bật `app.firebase.enabled=true`
với service-account thật, log không còn rơi về `LogOnlyFcmSender`.

---

## 7. 🔴 Hai thay đổi quy trình chủ dự án chốt ngày 2026-08-22 — áp dụng cho việc SAU NÀY

Chốt sau khi tham vấn agent `pm` về việc tiếp theo. **Không áp dụng ngược lại cho Stage 0 đã
xong** — chỉ áp dụng từ task backend/FE/mobile TIẾP THEO trở đi (thực tế sẽ rơi vào Stage 1).
Đã lưu chi tiết vào memory (`feedback_api_contract_docs`, `feedback_hold_fe_mo_testing`) — đọc
ở đó nếu cần lý do đầy đủ, đây chỉ tóm tắt để agent làm việc trong repo cũng thấy được.

1. **Backend phải xuất tài liệu endpoint, để FE/mobile không phải đọc source backend.** Từ
   task backend tiếp theo trở đi: kèm một file markdown tay (không phải `contracts/openapi.yaml`,
   không dựng generator — đúng tinh thần `contracts/README.md` hiện tại, chỉ cấm dựng toolchain
   generator sớm chứ không cấm ghi chú tay) liệt kê mỗi endpoint: method, path, request/response
   shape, header, status code. Khi giao việc FE/mobile sau đó, trỏ vào file này thay vì bảo agent
   tự đọc `.java` để suy ra shape (cách đã làm khi giao `customer_app` — sẽ không lặp lại nữa).
2. **Không để FE/mobile (agent) tự chạy test hay tự verify trên thiết bị nữa.** Từ giờ, việc
   `flutter test`, mở emulator/máy thật, click tay qua app để xác nhận — chủ dự án tự làm.
   Khi giao việc FE/mobile: chỉ dựng code, không đưa "chạy test/verify trên máy" vào phạm vi
   giao việc, và nói rõ trong báo cáo phần nào cần chủ dự án tự kiểm tra. Phạm vi này CHỈ áp
   cho FE/mobile — backend vẫn giữ nguyên bar test tự động (Testcontainers, `./gradlew test`)
   theo `AGENTS.md`/`docs/50-qa.md`, trừ khi chủ dự án nói rõ mở rộng sang cả backend.

---

## 9. 🔴 QUYẾT ĐỊNH MỚI (2026-08-22): ĐI TIẾP STAGE 2, STAGE 1 ĐỂ TREO

**Đây là quyết định trực tiếp của chủ dự án trong phiên làm việc**, không phải agent tự suy diễn.
Nguyên văn ý chính: *"tôi vẫn muốn tiếp tục tới Stage 2, còn các missing config thì khi nào tôi
available tôi sẽ tự thêm trực tiếp sau."*

Nghĩa là: **Stage 1 sẽ KHÔNG đóng** theo tiêu chí ở `missing_config.md` (VPS/domain/Firebase/R2
thật) trong thời gian tới. Chấp nhận **hai stage mở cùng lúc**. Mục 8 phía trên ("DỪNG AGENT",
"Stage 2 CHƯA bàn") là kết luận PM **trước** quyết định này — nay đã bị thay thế, giữ lại để tra
lịch sử chứ không còn hiệu lực.

⚠️ **Agent phiên sau đọc thấy `docs/70-stages.md` hoặc `docs/11-merchant-app.md` nói "Stage 2 chưa
làm bây giờ": đó là văn bản gốc, đã được override bởi đoạn này.** Cả hai file đó đã được thêm ghi
chú trỏ ngược về đây. Đừng dừng lại hỏi lại từ đầu — nhưng cũng đừng tự mở rộng ra ngoài phạm vi
sprint đã cắt bên dưới.

### Đã hỏi agent `pm` (2026-08-22, lần thứ hai) — phạm vi Stage 2 được cắt lại

Kết luận PM: **LÀM, nhưng cắt phạm vi**, không làm cả Stage 2. Lý do làm được: theo bất biến #1
của `docs/11-merchant-app.md`, merchant nhận đơn qua **hai kênh** — FCM *và* polling. Kênh polling
không phụ thuộc Firebase một dòng nào, phục vụ trực tiếp ưu tiên #1 "không bao giờ sót đơn".

**Ranh giới — chỉ 3 thứ thật sự cần credential thật:**
(a) lấy được device token thật, (b) chứng minh push đánh thức máy đang ngủ/bị OEM kill,
(c) mọi phép đo qua server thật/mạng 4G. Ngoài 3 thứ đó, làm được đàng hoàng ở local, không đoán mò.

**🔴 HAI LUẬT KIẾN TRÚC BẮT BUỘC** — để khi có credential chỉ phải "cắm dây", không viết lại:
1. merchant_app có **đúng MỘT** `IncomingOrderSink` (dedupe theo `order_id` + kêu chuông + ghi
   drift). Poller gọi nó; handler FCM sau này gọi đúng nó. **Cấm** chuông được kích hoạt từ hai chỗ.
2. `PushTokenProvider` là **interface**; flavor dev dùng `FakeTokenProvider` sinh token giả cố định
   → route đăng ký device chạy end-to-end thật được ngay, không chờ Firebase.

### Sprint đã cắt (thứ tự bắt buộc: backend trước)

Lý do backend trước: merchant_app hiện **không có gì để gọi** (`docs/90-api-contract.md` mới có 4
endpoint, chưa có sync đơn / nút đã-nhận-tiền / upload / write API nào). Thêm nữa theo mục 7, chủ
dự án là người verify tay FE/mobile — giao Flutter trước khi API có thật là ném việc verify vô ích.

- **2.1 backend nhận đơn** (chặn mọi thứ khác): `GET /v1/merchant/orders/sync?since=` (delta theo
  `updated_at`, `server_time`, `has_more`, ETag/304, cap ≤50 đơn/trang, DTO tóm tắt) + test
  cross-tenant · `POST /v1/merchant/orders/{id}/payment` (nút "Đã nhận tiền", **tách khỏi**
  `orderStatus`, **bắt buộc `Idempotency-Key`**) · bảng `device_tokens` + `POST/DELETE
  /v1/merchant/devices` + nối vào `OutboxBatchProcessor` (tra token theo tenant thay cho
  `deviceToken = null`; test rò rỉ token chéo tenant bằng fake sender) · **vòng đời JWT** (xem
  quyết định đã duyệt bên dưới) · cập nhật `docs/90-api-contract.md` **trong cùng commit**.
- **2.2 merchant_app nhận đơn**: đăng nhập → danh sách đơn qua polling → chi tiết → đổi trạng thái
  qua offline queue → foreground service + notification channel `new_order` + chuông stream ALARM
  lặp đến khi bấm xác nhận + full-screen intent + onboarding pin/autostart theo OEM + màn tự kiểm tra.
- **2.3 ảnh + catalog**: `POST /v1/merchant/media` (upload + resize, ghi `media.size_bytes/width/
  height`) · catalog write API (tạo/sửa món, giá, bật/tắt còn hàng, gắn ảnh, CRUD danh mục) ·
  `GET/PUT /v1/merchant/storefront` + **validate server-side theo `contracts/blocks.registry.json`**
  (whitelist prop/override, reject cái ngoài registry — rào chắn giữ cho một shop cấu hình sai
  không làm trắng màn hình app khách) · phía app: camera → nén/resize trên máy → hàng đợi upload.
- **2.4 studio** (rủi ro cao nhất, làm cuối): xem "lát cắt dọc" bên dưới.
- **2.5 (BỊ CHẶN)** nối FCM thật — ước tính ~1 ngày khi có credential, nếu tuân 2 luật kiến trúc.

### Studio — cắt dọc, không cắt ngang

**Lát cắt dọc #1 (làm trước, chỉ cái này):** kéo thả đổi thứ tự + bật/tắt block, có xem trước, lưu
được. **Không** sửa thuộc tính, **không** màu/font, **không** thêm block. Nhưng đi trọn vẹn: đăng
nhập → GET storefront của chính mình → sửa cục bộ → preview bằng **đúng `renderStorefront()`** của
`packages/ds_sdui` → PUT → server validate → customer_app kéo về thấy đổi thật.
Lý do: nếu preview lệch app khách, biết ngay ngày đầu, thay vì biết sau khi đã dựng 8 form thuộc tính.

Lát sau: (2) form ⚙ **sinh tự động** từ `overridable` — bar nghiệm thu là `category_row` có form
đầy đủ mà **không thêm một dòng code nào**, grep toàn repo không được có `if (blockType == ...)`.
(3) theme màu/radius/font. (4) nội dung (phụ thuộc 2.3). (5) "Thêm phần" + template.
Ba rào chắn không được bỏ: validate whitelist ở server · cấm renderer thứ hai · lưu có kiểm tra
tranh chấp (`updated_at`/version) + luôn giữ bản "tốt cuối cùng" để hoàn tác.

### Rủi ro đã biết khi nhảy stage — và cách giảm thiểu

- 🔴 **Vòng đời JWT (PM lo nhất, hơn cả FCM)**: app merchant chạy nền liên tục, mở ~50 lần/ngày.
  Token hết hạn mà không refresh = im lặng ngừng nhận đơn = **sót đơn**, đụng thẳng ưu tiên #1.
  → **Chủ dự án đã quyết** (xem mục 3 "Quyết định đã duyệt" số 12).
- 🔴 **FCM có thể không đến được trên máy OEM đích** — cả kiến trúc nhận đơn dựa vào kênh này.
  → Giảm thiểu: polling-first + `IncomingOrderSink` duy nhất + **chạy bài test 8 tiếng bằng
  polling-only ngay từ 2.2**. Bài test đó tốn *thời gian đồng hồ*, không tốn công — chạy sớm là
  cách rẻ nhất gỡ rủi ro OEM trước khi có Firebase.
- **Chưa từng đo `orders/sync` qua 4G/server thật** → chốt ngân sách ngay từ khi thiết kế: cap
  ≤50 đơn/trang, DTO tóm tắt (chi tiết item gọi riêng), ETag/304, có test khẳng định lần poll rỗng
  gần như không tốn byte.
- **Shape lưu ảnh quyết theo đĩa local rồi R2 khác semantics** → URL **luôn do server trả**, client
  không bao giờ tự ghép; chạy test upload trên cả `mode=local` và MinIO (đường test có sẵn Stage 1).
- **`tenant.business_day_start` chưa dùng ở đâu** → làm đúng ngay từ đầu khi động tới báo cáo/tổng
  kết, đừng tính theo nửa đêm rồi viết lại.
- Chỉ là chậm trễ, chấp nhận được: Crashlytics merchant_app, tinh chỉnh nội dung/âm thanh push,
  hiệu năng TLS/CDN, UX cảnh báo quota, mọi thứ iOS.

### Hệ quả phải chấp nhận
**Stage 2 sẽ KHÔNG đóng được** — bar hoàn thành (`docs/70-stages.md`: "chạy nền 8 tiếng trên máy
Xiaomi/Oppo thật, gửi đơn lúc 3 giờ sáng, chuông vẫn kêu") cần FCM thật. Việc còn treo ghi trong
`missing_config.md` — **một file trạng thái duy nhất, đừng đẻ file thứ ba**.
