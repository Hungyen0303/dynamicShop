# progress.md — Stage 0 ĐÃ ĐÓNG

> File này để agent/session sau đọc và tiếp tục ngay, không cần dò lại từ đầu.
> Giữ lại làm hồ sơ build Stage 0 — xoá khi nào chủ dự án thấy không cần tra lại nữa.

**Cập nhật lần cuối (2026-08-22):** Chủ dự án đã tự tay đi lại flow và xác nhận **OK** — điều
kiện cuối cùng của Stage 0 (`docs/70-stages.md` + `INIT.md` mục "✅ Stage 0 xong khi") đã đạt.
**Stage 0 coi như đóng.** Cả backend và customer_app đã xong thật, đã review, đã commit + push
(xem mục 1, 2 để tra chi tiết kỹ thuật nếu cần).

Đã hỏi agent `pm` "làm gì tiếp theo" trước khi đóng Stage 0 — xác nhận không có việc kỹ thuật
nào bị bỏ sót, đúng nguyên tắc "local trước, mọi thứ khác sau". Nhân dịp đó, chủ dự án chốt 2
thay đổi quy trình cho việc SAU NÀY — xem mục 5.

**Bước tiếp theo:** phạm vi Stage 1 (VPS, Firebase/FCM, R2 ảnh...) CHƯA được bàn hay quyết —
theo đúng luật ở `AGENTS.md`/`docs/70-stages.md`, agent không tự nhảy stage. Hỏi chủ dự án
trước khi bắt đầu bất cứ việc gì thuộc Stage 1, kể cả để lên kế hoạch.

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

## 4. KHÔNG được tự ý làm (nhắc lại từ AGENTS.md, phòng agent sau quên)
- Không thêm dependency ngoài: web/webmvc, data-jpa, security, validation, postgresql, flyway,
  actuator, testcontainers (backend). Với customer_app: xem `INIT.md` mục 5, hỏi trước khi thêm
  gì ngoài Flutter SDK + `ds_*` packages nội bộ.
- Không làm gì thuộc Stage 1+ (FCM thật, VPS, CI/CD, merchant_app, admin web, Zalo).
- Không `git push --force`, không amend commit đã có.
- Không tắt/`@Disabled` test để né lỗi — thấy đỏ thì sửa nguyên nhân.

---

## 5. 🔴 Hai thay đổi quy trình chủ dự án chốt ngày 2026-08-22 — áp dụng cho việc SAU NÀY

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
