# progress.md — Stage 0, tình trạng bàn giao

> File này để agent/session sau đọc và tiếp tục ngay, không cần dò lại từ đầu.
> Xoá file này khi Stage 0 hoàn tất và đã giải thích được toàn bộ flow (theo `INIT.md`).

**Cập nhật lần cuối:** backend Stage 0 đã xong thật (24/24 test xanh, đã curl-test qua HTTP
thật, đã commit + push). Việc tiếp theo là `customer_app` — xem mục 2.

---

## 0. Đọc gì trước khi làm tiếp

Theo `AGENTS.md` mục 0: task đụng customer_app → đọc `docs/00-context.md` + `docs/70-stages.md`
(luôn luôn) + `docs/10-customer-app.md`. Đọc thêm `apps/customer_app/INIT.md`.

---

## 1. Backend Stage 0 — ĐÃ XONG, đã commit, đã push

Git log hiện tại (`main`, tất cả đã push lên GitHub):

```
<hash mới nhất> be: sửa GUC tenant dùng doBegin() thay AOP pointcut — repository gọi trực
                    tiếp không qua service từng bị mất bảo vệ RLS
0357c45          be: hoàn thành backend Stage 0 — tenant isolation, auth, storefront, order,
                    idempotency, outbox
8cd1637          ct: thêm contracts/storefront.schema.json
bed7b8f          be: khởi tạo Gradle project (Spring Boot 4.1.1, Java 21)
1c23c54          chore: khởi tạo skeleton dự án
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

## 2. 🟡 Việc tiếp theo — customer_app (Stage 0, chưa bắt đầu)

Theo thứ tự trong `INIT.md`: sau backend là `customer_app`. Chưa có gì được làm ở đây.

Đề xuất: giao cho subagent `mobile-engineer`. Đọc trước `docs/10-customer-app.md`,
`apps/customer_app/INIT.md`, `docs/flavors.md` (nếu có). Việc cần làm theo `INIT.md` mục 5:
- `flutter create` với 3 flavor môi trường (dev/staging/prod), CHƯA melos (dùng `path:` deps)
- Trỏ về `http://10.0.2.2:8080` (emulator Android nhìn localhost máy host)
- Màn hình dev đổi giữa mock shop A (`bun-co-ba`) / B (`tra-sua-ngoc`) — cần cho bước 3 của bar
  hoàn thành (`docs/70-stages.md`)
- `assets/default_storefront.json` — fallback tầng 3 (xem "Fallback ba tầng" trong
  `docs/10-customer-app.md`)
- SDUI mức 2, dùng `ds_blocks`/`ds_sdui`/`ds_tokens` (packages/ hiện chỉ có `.gitkeep`, cần dựng
  cùng lúc)

Khi giao việc cho subagent, viết prompt CHI TIẾT, TỰ CHỨA như đã làm với `backend-engineer`
(xem lịch sử hội thoại phiên trước nếu còn, hoặc tự đọc lại các doc trên rồi viết mới) — agent
mới không có ký ức phiên này.

Sau khi customer_app xong: nạp `V900__mock_tenant_a.sql`/`V901__mock_tenant_b.sql` (đã có sẵn
trong `backend/src/main/resources/db/migration-local/`) và chạy đủ 8 bước bar hoàn thành trong
`docs/70-stages.md` bằng tay để xác nhận Stage 0 THẬT SỰ xong.

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

## 4. KHÔNG được tự ý làm (nhắc lại từ AGENTS.md, phòng agent sau quên)
- Không thêm dependency ngoài: web/webmvc, data-jpa, security, validation, postgresql, flyway,
  actuator, testcontainers (backend). Với customer_app: xem `INIT.md` mục 5, hỏi trước khi thêm
  gì ngoài Flutter SDK + `ds_*` packages nội bộ.
- Không làm gì thuộc Stage 1+ (FCM thật, VPS, CI/CD, merchant_app, admin web, Zalo).
- Không `git push --force`, không amend commit đã có.
- Không tắt/`@Disabled` test để né lỗi — thấy đỏ thì sửa nguyên nhân.
