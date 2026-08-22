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

## ⚠️ Cần chủ dự án quyết: policy RLS nên chịu được GUC rỗng?

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

Mình nghiêng về hướng thứ hai + giữ nguyên `@Transactional` ở trên như tuyến phòng thủ chính, nhưng
đây là quyết định về hành vi an toàn nên không tự quyết.

## Bảng `device_tokens` unique THEO TENANT, không unique toàn cục

`UNIQUE (tenant_id, token)`. Đúng hơn về mặt mô hình sẽ là unique toàn cục (một token = một máy = một
chủ), nhưng app **không thể tự dọn dòng của tenant khác**: RLS chặn, và role BYPASSRLS chỉ được dùng
trong package `admin/`. Unique toàn cục sẽ khiến việc đăng ký chết bằng lỗi ràng buộc 23505 không diễn
giải nổi cho người dùng.

Hệ quả thật: một chiếc điện thoại được đăng nhập ở hai quán khác nhau sẽ nhận push của **cả hai**. Đúng
nếu là người có hai quán, sai nếu là máy chuyển tay. Cách dọn đúng cho token cũ là phản hồi
`UNREGISTERED` của chính FCM — cần Firebase thật, sprint 2.5. Ghi nhận giới hạn thay vì giả vờ đã giải
quyết.

## `server_time` / `has_more` là snake_case, lệch với phần còn lại của API

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
