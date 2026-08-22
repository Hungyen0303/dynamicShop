# aware.md — Quyết định kỹ thuật cần chủ dự án tự check lại

> Không phải lỗi — nhưng đây là chỗ agent tự quyết vì không có ai hỏi ngay lúc đó, hoặc sự cố
> đáng biết. Đọc qua rồi xác nhận hoặc yêu cầu đổi hướng. Xem `progress.md` mục Stage 1 để biết
> bối cảnh phê duyệt, `missing_config.md` để biết cấu hình còn thiếu.

---

## 🔴 Sự cố thật lúc test hạ tầng — đã xử lý, không mất dữ liệu

Lúc test `infra/docker/docker-compose.prod.yml` lần đầu, file này và `docker-compose.yml` (dev)
nằm cùng thư mục nên Docker Compose lấy project name mặc định giống nhau (tên thư mục), và cả hai
đều có service key `postgres` → Compose coi đó là MỘT service, chạy `docker compose up` cho file
prod đã **"recreate" (xoá) mất container Postgres dev đang chạy thật**. Volume dữ liệu
(`docker_ds-pgdata`) may mắn không bị xoá — phục hồi ngay bằng
`docker compose -f docker-compose.yml up -d`, verify lại 2 mock tenant (`bun-co-ba`,
`tra-sua-ngoc`) còn nguyên qua `psql`. Đã sửa gốc: ghim `name: ds-prod` ở đầu
`docker-compose.prod.yml` để không bao giờ đụng namespace với dev nữa. Nêu ra để chủ dự án biết
— không có gì cần làm thêm, nhưng đây là lời nhắc: **đừng đặt hai file compose cùng thư mục có
service key trùng tên** trong tương lai.

## Không tạo endpoint HTTP upload ảnh

`app.storage.mode=s3`/`ImageStorageService` chỉ là hạ tầng — chưa có route HTTP nào gọi tới, vì
chưa có tính năng nào cần nó (chụp ảnh món ăn là `merchant_app`, Stage 2). Quyết định: dựng sẵn
adapter để Stage 2 cắm vào chạy ngay, không dựng thừa route chưa ai dùng. Nếu muốn có upload ảnh
sớm hơn Stage 2 (vd để nhập ảnh sản phẩm qua admin/tay), cần nói rõ — đây là scope call tự đưa ra.

## Chọn AWS SDK v2 (`software.amazon.awssdk:s3`) cho R2/MinIO

R2 và MinIO đều tương thích giao thức S3 nên dùng chung SDK. Đây là dependency mới (cùng
`com.google.firebase:firebase-admin` và `org.testcontainers:testcontainers-minio` cho test) —
nằm trong phạm vi Stage 1 đã duyệt (`docs/70-stages.md` liệt kê Firebase + R2), không hỏi lại
riêng từng dependency.

## `docker-compose.prod.yml` mặc định cổng 8080/8443 khi test local

Không dùng 80/443 để tránh xung đột với dịch vụ khác đang chạy trên máy dev. Deploy VPS thật thì
đổi `HTTP_PORT=80`/`HTTPS_PORT=443` trong `.env.prod` (xem `missing_config.md`).

## Backend Dockerfile cài thêm `wget` ở runtime image

Chỉ để `HEALTHCHECK`/Compose healthcheck gọi `/actuator/health` — image JRE gốc
(`eclipse-temurin:21-jre`) không có sẵn curl/wget. Tăng nhẹ kích thước image, đổi lại có
healthcheck thật thay vì đoán container sống.

## ~~FCM chưa gửi được push nào dù bật Firebase thật~~ — đã hết hiệu lực một phần (sprint 2.1)

~~`deviceToken` luôn `null` trong `OutboxBatchProcessor` vì chưa có bảng đăng ký device token
merchant (Stage 2).~~ Sprint 2.1 đã thêm bảng `device_tokens` + route đăng ký, và
`OutboxBatchProcessor` giờ fan-out tới token thật của đúng tenant. Phần **vẫn còn đúng**: chưa gửi
được push thật vì `FcmSender` còn là `LogOnlyFcmSender` — chờ service-account Firebase
(`missing_config.md` mục 3). Xem phần "Sprint 2.1" cuối file.

## customer_app: đã nối Firebase Crashlytics (2026-08-22), có build-check thật

Lượt trước dừng ở mức tài liệu vì lo ngại thêm plugin Gradle mù có thể vỡ build. Lượt này (được
yêu cầu "continue") đã làm tiếp theo cách an toàn hơn suy nghĩ ban đầu — hoá ra rủi ro thật chỉ
nằm ở việc **apply plugin Gradle `google-services`/`firebase-crashlytics`** khi thiếu file config
(plugin đó fail build ngay bước config nếu thiếu file), KHÔNG nằm ở việc thêm pub dependency
`firebase_core`/`firebase_crashlytics` hay gọi `Firebase.initializeApp()` phía Dart — hai plugin
Gradle giờ chỉ apply khi `android/app/build.gradle.kts` tìm thấy `google-services.json` thật (biến
`hasGoogleServicesConfig`), nên thiếu file thì build không đổi.

**Đã verify bằng build thật, không chỉ đọc code** — vì đây là thay đổi Gradle, đọc code không đủ
để chắc chắn: `flutter pub get` (resolve OK), `flutter analyze` (sạch, sửa 1 lint import thừa),
`flutter build apk --flavor dev -t lib/main_dev.dart --debug` (build thành công, ra APK). Đây là
**compile-check tối thiểu** (build, không chạy) — cân nhắc là cần thiết vì thay đổi Gradle không
thể tin chỉ bằng đọc code, khác với sửa Dart thuần. Nếu chủ dự án thấy việc này (chạy `flutter
build`) đã vượt quá phạm vi "chỉ dựng code" đã chốt ở `progress.md` mục 7, nói rõ để lần sau dừng
đúng ở mức không build gì cả, chỉ viết code + để chủ dự án tự build/verify hoàn toàn.

Chưa build thử flavor `staging`/`prod` (logic guard giống hệt dev, cùng kết quả — bỏ qua để đỡ
tốn thời gian) và chưa đụng iOS (máy Windows không build được, xem `missing_config.md` mục 6).

## Hai lượt agent trước bị chặn vì tưởng Stage 1 chưa duyệt — đã xử lý, nêu lại để minh bạch

Hai agent con (chạy fresh, không có context hội thoại) đã dừng lại đúng lúc vì thấy bản commit mới
nhất của `progress.md` (trên GitHub) nói Stage 1 "chưa được bàn", trong khi phê duyệt thật của chủ
dự án chỉ nằm trong hội thoại + một sửa `progress.md` **chưa commit**. Agent thứ hai còn nghi ngờ
đúng đắn rằng đó có thể là một "approval trail" giả mạo. Đây là phản xạ an toàn đúng, không phải
lỗi của agent — vấn đề là giao việc yêu cầu-phê-duyệt cho agent hoàn toàn mới không có cách xác
minh nguồn gốc. Phần việc còn lại (Dockerfile, compose, Caddyfile, fix test) do chính phiên làm
việc trực tiếp với chủ dự án tự làm, không giao thêm cho agent mới. `progress.md` hiện vẫn ở dạng
uncommitted — nếu chủ dự án muốn approval này thành bản ghi bền vững (để agent sau không hỏi lại),
cân nhắc tự commit (mình không tự commit vì chỉ commit khi được yêu cầu).

---

# Sprint 2.1 — backend nhận đơn (2026-08-22)

## 🔴 Bẫy thật đã gặp và sửa: query dẫn xuất của Spring Data KHÔNG có transaction → mất GUC tenant

Sáu test đỏ khi chạy lần đầu với lỗi `invalid input syntax for type uuid: ""` từ Postgres. Nguyên nhân
không phải ở test: `SimpleJpaRepository` có `@Transactional(readOnly = true)` ở mức class, nhưng nó
**chỉ phủ các method kế thừa** (`save`, `findById`, `findAll`) — **query dẫn xuất khai báo trên
interface của mình (`findByToken`, `findByRevokedAtIsNull`, `findByUpdatedAtGreaterThanEqual...`) chạy
KHÔNG có transaction nào**. Không transaction → `TenantAwareJpaTransactionManager.doBegin()` không
chạy → GUC `app.tenant_id` không được set → policy RLS đánh giá `current_setting('app.tenant_id',
true)::uuid` trên chuỗi rỗng và ném lỗi.

Đây đúng là họ lỗi mà `TenantAwareJpaTransactionManager` được viết ra để chống (xem Javadoc của nó về
việc AOP pointcut không bắt được repository proxy) — chỉ khác đường vào. **Production không bị ảnh
hưởng**: mọi lời gọi thật đều nằm trong service `@Transactional` nên transaction đã có sẵn; chỉ lời
gọi repository trực tiếp (test, và bất kỳ code nào sau này) mới lộ.

**Đã sửa**: đặt `@Transactional(readOnly = true)` trên TỪNG method dẫn xuất của `DeviceTokenRepository`,
`OrderRepository`, `OrderItemRepository`. Cố ý không đặt ở mức interface — mức interface sẽ phủ cả
`save()` kế thừa và biến mọi lệnh ghi thành read-only.

## ~~⚠️ Cần chủ dự án quyết: policy RLS nên chịu được GUC rỗng?~~ → ĐÃ CHỐT, xem phần Sprint 2.1b

Phát hiện kèm theo, CHƯA sửa vì nó là thay đổi schema trên 11 bảng và đáng để chủ dự án quyết:

Policy hiện tại (V1) là `USING (tenant_id = current_setting('app.tenant_id', true)::uuid)`. Khi GUC
chưa từng được set trong session → `current_setting` trả `NULL` → policy lọc rỗng, yên lặng. Nhưng sau
khi một transaction đã set rồi kết thúc, Postgres để lại **chuỗi rỗng** chứ không phải NULL → `::uuid`
**ném lỗi**. Cùng một dòng code, hành vi khác nhau tuỳ trạng thái connection pool — đúng họ "bẫy
connection pool" trong `.claude/skills/tenant-isolation`.

Hai hướng, cần chọn một:
- **Để nguyên** — thiếu tenant thì nổ to, dễ phát hiện. Nhược điểm: lỗi 500 với thông báo DB khó hiểu,
  và không nhất quán (lúc nổ lúc không).
- **V3 đổi policy sang `NULLIF(current_setting('app.tenant_id', true), '')::uuid`** — thiếu tenant thì
  luôn lọc rỗng, nhất quán. Nhược điểm: lỗi quên set tenant trở nên im lặng hơn.

~~Mình nghiêng về hướng thứ hai~~ — **đã bị bác, và bác đúng.** Chủ dự án chốt: hoãn thi hành, và
khi làm thì chọn hướng **"luôn nổ"**, KHÔNG dùng NULLIF. Lý do đầy đủ ở phần Sprint 2.1b cuối file.

## Bảng `device_tokens` unique THEO TENANT, không unique toàn cục

`UNIQUE (tenant_id, token)`. Đúng hơn về mặt mô hình sẽ là unique toàn cục (một token = một máy = một
chủ), nhưng app **không thể tự dọn dòng của tenant khác**: RLS chặn, và role BYPASSRLS chỉ được dùng
trong package `admin/`. Unique toàn cục sẽ khiến việc đăng ký chết bằng lỗi ràng buộc 23505 không diễn
giải nổi cho người dùng.

Hệ quả thật: một chiếc điện thoại được đăng nhập ở hai quán khác nhau sẽ nhận push của **cả hai**. Đúng
nếu là người có hai quán, sai nếu là máy chuyển tay. Cách dọn đúng cho token cũ là phản hồi
`UNREGISTERED` của chính FCM — cần Firebase thật, sprint 2.5. Ghi nhận giới hạn thay vì giả vờ đã giải
quyết.

## ~~`server_time` / `has_more` là snake_case~~ → ĐÃ ĐỔI sang camelCase ở sprint 2.1b

`docs/30-backend.md` chốt shape `{ orders, server_time, has_more }` từ trước, trong khi mọi field khác
toàn hệ thống là camelCase (kể cả các field bên trong `orders[]`). Đã **giữ đúng văn bản** thay vì tự ý
chuẩn hoá. Nếu chủ dự án muốn đổi sang `serverTime`/`hasMore` cho nhất quán thì **bây giờ là lúc rẻ
nhất** — merchant_app chưa viết, chưa có client nào phụ thuộc. Sau sprint 2.2 thì đổi sẽ tốn hơn.

## `/payment` nhận cả 4 giá trị PaymentStatus, không chỉ PAID

Nút trên app hiện chỉ có "Đã nhận tiền", nhưng endpoint chấp nhận `UNPAID|PARTIAL|PAID|REFUNDED` và để
state machine quyết chuyển nào hợp lệ. Lý do: hoàn tiền và thu một phần là chuyện có thật ở quán, state
machine đã biết luật rồi, chặn bớt ở tầng HTTP chỉ để mở lại ở sprint sau là việc thừa. Nếu muốn khoá
cứng chỉ cho `PAID` ở giai đoạn này, nói một tiếng.

## Đổi payment vẫn enqueue outbox (push)

Người bấm nút "Đã nhận tiền" cũng là người sẽ nhận push — nghe có vẻ thừa. Vẫn làm vì một quán thường
có nhiều máy (điện thoại chủ + máy nhân viên) và máy còn lại cần biết tiền đã thu để không đòi khách
lần hai. Nếu thấy ồn, sprint 2.2 có thể lọc ở phía client theo `actor_id`.

## JWT 30 ngày — thu hồi phiên hiện KHÔNG có cách nào ngoài đổi secret

Hệ quả trực tiếp của quyết định #12 (TTL dài, không refresh token). Chưa có bảng phiên, nên nếu một
máy bị mất/bị lộ token, cách duy nhất là đổi `APP_JWT_SECRET` — và việc đó đá văng **toàn bộ** merchant
của mọi tenant cùng lúc. Chấp nhận được ở quy mô hiện tại; nêu ra để không ai bất ngờ lúc cần.

---

# Sprint 2.1b — sửa lỗi mất đơn + dọn nợ trước khi mở Flutter (2026-08-22)

Sprint này sinh ra từ việc chủ dự án yêu cầu gọi agent `pm` check lại tiến độ. PM tự đọc code và
tìm ra một lỗi mà cả phiên làm việc lẫn `aware.md` đều bỏ sót — nêu ở đây vì nó là bằng chứng cho
thấy việc gọi PM rà lại ở ranh giới sprint có giá trị thật, không phải thủ tục.

## 🔴 Lỗi mất đơn IM LẶNG lọt qua sprint 2.1 — đã sửa, có regression test

`OrderSyncService` cũ trả `Instant.now()` lấy **sau** khi query xong làm mốc `server_time`.
`Order.updatedAt` do `@UpdateTimestamp` gán lúc **flush** nhưng transaction commit **sau đó**, nên
có cửa sổ mà một đơn đã mang `updated_at = T1` nhưng chưa nhìn thấy được từ transaction khác. Poll
ở T2 (T1 < T2 < T3=commit) không thấy đơn đó nhưng vẫn trả mốc T2 → lần poll sau lọc `>= T2` → **đơn
đó không bao giờ xuất hiện lại**. Không log, không lỗi, không ai biết.

Nguy hiểm gấp đôi vì `docs/90-api-contract.md` khi đó **đang dạy client lấy `since` từ
`server_time`** — tức là cái sai nằm trong luật client, và nếu để tới sprint 2.2 thì merchant_app
sẽ được viết đúng theo luật sai đó.

**Đã sửa**: lấy mốc **trước** khi query và **trừ biên an toàn 60 giây**
(`OrderSyncService.WATERMARK_SAFETY_MARGIN`). Cái giá là client nhận lặp các đơn trong cửa sổ 60
giây — vô hại vì client bắt buộc dedupe theo `id` rồi (bất biến #4), và ETag vẫn cho 304 bình
thường khi không có gì đổi.

**Đã chứng minh test bắt được lỗi, không chỉ viết test rồi tin**: tạm đảo ngược bản sửa về
`Instant.now()` và chạy lại → đúng 2 test mới đỏ
(`server_time_luon_lui_ve_qua_khu_du_xa_de_khong_bo_sot_don`,
`don_vua_tao_van_con_o_lan_poll_sau_khi_dung_server_time_lam_since`), 9 test còn lại vẫn xanh. Rồi
khôi phục.

Không mô phỏng được race thật bằng đồng thời trong test: `AbstractIntegrationTest` ghim
`maximum-pool-size=1` (cố ý, để bắt lỗi rò rỉ GUC qua pool), nên giữ một transaction mở rồi query từ
connection khác sẽ deadlock. Hai test trên kiểm **bất biến** thay vì kiểm race — đủ chặt và không
mong manh.

## 403 → 401 cho mọi request thiếu/sai xác thực

Spring mặc định trả **403** khi thiếu `Authorization` (`Http403ForbiddenEntryPoint`). Sai về HTTP,
và ở dự án này là lỗi mất đơn: quyết định #12 dựng cả vòng đời phiên trên luật "gặp 401 thì tự đăng
nhập lại", nên một lỗi rơi header ở client (dễ xảy ra khi foreground service khởi động lại lúc 3
giờ sáng) sẽ nhận 403, không nằm trong luật, và **im lặng ngừng nhận đơn**.

Đã thêm `JsonAuthenticationEntryPoint` → mọi trường hợp đều ra `401` với body
`{"code":"UNAUTHENTICATED"}`, cùng shape lỗi với phần còn lại của API. `JwtAuthenticationFilter`
cũng đổi từ `response.sendError()` (trang HTML của container) sang cùng JSON đó.

**Đây là thay đổi hành vi API.** Không có client nào đang phụ thuộc (merchant_app chưa viết,
customer_app chỉ dùng public plane), nhưng ghi lại để không ai bất ngờ. Ba test cũ khẳng định 403
đã sửa theo.

## `server_time`/`has_more` → `serverTime`/`hasMore`

Đã đổi sang camelCase như PM khuyến nghị, sửa cả `docs/30-backend.md` và
`docs/90-api-contract.md` trong cùng commit. Làm lúc này vì chi phí gần bằng không; sau sprint 2.2
thì phải sửa cả client và chủ dự án phải verify tay lại.

## Policy RLS khi GUC rỗng — chủ dự án đã chốt hướng, HOÃN thi hành

Chủ dự án chốt qua `AskUserQuestion` (2026-08-22): **hoãn**, và khi làm thì chọn hướng **"luôn nổ"**
(`current_setting('app.tenant_id')::uuid`, bỏ `missing_ok`) — KHÔNG dùng `NULLIF`.

Ghi lại lập luận vì nó ngược với hướng mà chính mình nghiêng về lúc đầu, và PM đúng: `NULLIF` biến
"quên set tenant" thành **danh sách đơn rỗng, im lặng**, trên đúng endpoint mà im lặng nghĩa là sót
đơn. Nổ to thì có người sửa; rỗng im lặng thì không ai biết. Tuyến phòng thủ chính vẫn là
`@Transactional` trên query dẫn xuất (đã làm ở 2.1) — cái này chỉ là hàng rào cuối.

**Xem lại trước sprint 2.3** — đó là sprint đẻ ra nhiều repository/write path mới, tức là lúc dễ
giẫm lại mìn nhất. Chi phí sửa 11 policy y hệt nhau dù làm bây giờ hay sau.

## Ba luật cho sprint 2.2 sinh ra từ chính thiết kế 2.1 — PM nêu, chưa ai code

Không phải việc backend, nhưng phải nhớ khi viết merchant_app:

1. **Chuông chỉ kêu khi `order_id` chưa từng thấy VÀ `orderStatus == PENDING`.** Vì `/payment` và
   `/transition` đều làm `updated_at` đổi → đơn cũ quay lại trong `/sync`; cộng với bộ lọc `>=` và
   cửa sổ chồng lấn 60 giây, đơn cũ xuất hiện lại là chuyện thường xuyên. Không có luật này thì
   chuông kêu khi chính chủ quán bấm "Đã nhận tiền" trên máy kia — và chủ quán sẽ tắt chuông.
2. **Bảng dedupe phải nằm trong drift, không phải RAM.** Foreground service bị OEM kill rồi restart
   lúc 3 giờ sáng mà dedupe nằm trong bộ nhớ thì toàn bộ đơn cũ sẽ reo lại một lượt.
3. **Phải test được đường tự-đăng-nhập-lại.** TTL 30 ngày nghĩa là bug ở đường đó sẽ không tự lộ
   ra khi test tay — ép bằng `app.jwt.expiration-minutes: 2` ở local một buổi.
