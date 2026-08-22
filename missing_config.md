# missing_config.md — Cấu hình thật còn thiếu cho Stage 1

> Mọi thứ dưới đây đã có **fallback an toàn**: không điền gì thì app chạy y hệt Stage 0 (đã verify
> `./gradlew clean test` 29/29 xanh + chạy thật `docker compose ... up` local). Danh sách này là
> việc **chủ dự án tự làm sau** khi có tài khoản/thiết bị thật. Xem `progress.md` mục 6, 8 để biết
> bối cảnh phê duyệt Stage 1 và kết luận PM, `aware.md` để biết quyết định kỹ thuật nào cần tự
> check lại.

## 🔴 Trạng thái (2026-08-22, cập nhật lần 2) — đọc trước khi làm bất cứ mục nào

**Chủ dự án đã quyết: ĐI TIẾP STAGE 2, để 6 mục dưới đây TREO, tự điền sau khi rảnh.** Xem
`progress.md` mục 9 để biết phạm vi Stage 2 đã được cắt lại thế nào. Nghĩa là **hai stage mở cùng
lúc** — Stage 1 treo chờ credential, Stage 2 đang làm phần không cần credential.

Mọi mục dưới đây đều cần chủ dự án tự đi lấy (tài khoản, thanh toán, xác minh danh tính), không
phải việc code. Agent phiên sau: **đừng tự đi "làm thử" hay đoán giá trị cho các mục này** — chỉ
dùng file này để biết mục nào đã điền, mục nào còn trống, việc kế tiếp là gì cho mục đó (mỗi mục
có ghi "→ agent làm gì tiếp khi có").

Đánh dấu `[x]` khi chủ dự án đã điền xong một mục, để agent sau biết mục nào sẵn sàng làm tiếp:

- [ ] 1. VPS + domain thật
- [ ] 2. Secrets `.env.prod` thật (đổi khỏi giá trị mẫu)
- [ ] 3. Firebase service-account thật (backend FCM)
- [ ] 4. Cloudflare R2 credential thật
- [ ] 5. Kế hoạch backup Postgres
- [ ] 6. Firebase project + `google-services.json`/`GoogleService-Info.plist` (customer_app)

**Tiêu chí đóng Stage 1 thật (PM chốt)** — khi đủ 1+2+3+6 ở trên, agent cần verify đủ 3 điều này
trước khi báo Stage 1 xong:
1. Deploy `docker-compose.prod.yml` lên VPS thật, domain thật trỏ đúng IP,
   `curl https://<domain>/actuator/health` → `200 UP` qua **TLS Let's Encrypt thật** (không phải
   CA nội bộ như lúc test local).
2. `customer_app` build flavor trỏ base URL về domain thật, chạy trên máy Android thật qua **mạng
   4G** (không wifi/localhost), đặt đơn thành công, verify DB trên VPS có đơn đúng tenant.
3. Bật `app.firebase.enabled=true` với service-account thật, xác nhận log KHÔNG rơi về
   `LogOnlyFcmSender` nữa (dù chưa gửi push thật được — chưa có device token, đúng vì
   merchant_app/Stage 2 chưa làm, xem mục 3 bên dưới).

~~**Đừng tự làm thêm**: resize ảnh lúc upload / endpoint HTTP upload ảnh — chưa có consumer thật.
Cũng đừng bắt đầu bàn/lên kế hoạch Stage 2 trước khi 3 tiêu chí đóng Stage 1 đạt đủ.~~
🔴 **Đã hết hiệu lực (2026-08-22)** — chủ dự án quyết đi tiếp Stage 2, và Stage 2 CHÍNH LÀ consumer
của upload ảnh. Upload + resize nay nằm trong sprint 2.3 (`progress.md` mục 9), dùng **Thumbnailator**
(đã duyệt). Vẫn giữ nguyên hai cảnh báo còn đúng: **không làm video/transcode** ở lát này, và
**không enforce quota** (chỉ ghi `size_bytes` đúng từ ngày đầu — backfill sau rất đau).

---

## 🟡 Việc Stage 2 bị CHẶN vì thiếu credential (thêm 2026-08-22)

Ghi ở đây thay vì tạo file trạng thái thứ ba. Đây là phần của Stage 2 **không** làm được cho tới
khi mục 3 + mục 6 phía trên được điền:

- **Lấy device token thật** — `getToken()` của FCM cần `google-services.json` thật. Cho tới lúc đó
  merchant_app flavor dev dùng `FakeTokenProvider` (token giả cố định) để route đăng ký device vẫn
  chạy end-to-end thật được. Xem luật kiến trúc #2 ở `progress.md` mục 9.
- **Chứng minh push đánh thức máy đang ngủ / bị OEM kill** — kịch bản test 1/2/6/7 trong
  `docs/11-merchant-app.md` ở dạng đầy đủ. Cho tới lúc đó chạy bài test 8 tiếng bằng
  **polling-only** (vẫn gỡ được phần lớn rủi ro OEM).
- **Đo latency `orders/sync` qua 4G / server thật** — cần mục 1 (VPS + domain).
- **Crashlytics cho merchant_app** — dùng lại đúng khuôn guard đã chứng minh ở customer_app.

**→ Hệ quả: Stage 2 KHÔNG đóng được** cho tới khi có FCM thật (bar hoàn thành trong
`docs/70-stages.md` là "chạy nền 8 tiếng trên Xiaomi/Oppo thật, gửi đơn lúc 3 giờ sáng, chuông vẫn
kêu"). Đây là đánh đổi đã biết và đã được chủ dự án chấp nhận, không phải thiếu sót.

---

## 1. VPS + domain thật

- Nhà cung cấp VPS (khuyến nghị Singapore, gần VN — xem `infra/docs/README.md`), IP, SSH key.
- Domain thật (vd `api.dynamicshop.vn`) + DNS A record trỏ về IP VPS.
- Mở port 80/443 trên firewall VPS.

Khi có: copy `infra/docker/docker-compose.prod.yml`, `infra/docker/Caddyfile`,
`infra/docker/.env.prod.example` lên VPS, `cp .env.prod.example .env.prod`, điền domain thật vào
`DOMAIN=`, đổi `HTTP_PORT=80`/`HTTPS_PORT=443`, rồi
`docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build`. Caddy tự xin
chứng chỉ Let's Encrypt thật (khác với test local — xem ghi chú trong `Caddyfile`).

**Đã test thật ở local** (2026-08-22): build image, chạy full stack, curl qua Caddy
`https://localhost:8443/actuator/health` → `200 {"status":"UP"}`. Chuỗi container → backend →
postgres chạy đúng, chỉ còn thiếu VPS/domain thật.

**→ Khi có VPS + domain, agent làm:** SSH lên VPS, cài Docker, copy 3 file compose/Caddyfile/env
example, deploy như hướng dẫn trên, verify `curl https://<domain thật>/actuator/health` → `200 UP`
qua Let's Encrypt thật (khác test local dùng `-k` vì CA nội bộ). Đây là điều kiện #1 trong "Tiêu
chí đóng Stage 1" ở đầu file.

## 2. Secrets cho `.env.prod` (KHÔNG dùng giá trị mẫu/dev)

Trong `infra/docker/.env.prod.example`, các giá trị placeholder PHẢI đổi trước khi deploy thật:
- `POSTGRES_PASSWORD`, `APP_USER_PASSWORD`, `APP_ADMIN_PASSWORD` — mật khẩu DB thật.
- `APP_JWT_SECRET` — secret ký JWT thật, **không dùng chung** secret dev đang commit sẵn trong
  `backend/src/main/resources/application-local.yml` (đã có ghi chú "DỪNG LẠI" ở đó từ Stage 0).

**→ Khi chủ dự án đã điền `.env.prod` thật:** agent chỉ cần xác nhận file KHÔNG bị commit nhầm
(`git status` phải không thấy `.env.prod`, đã có trong `.gitignore`) trước khi deploy mục 1.

## 3. Firebase (FCM) — phía backend

Property: `app.firebase.enabled` (mặc định `false`) và `app.firebase.service-account-path`.

Cần: tạo Firebase project thật → Project Settings → Service Accounts → generate private key (file
JSON). Đặt file đó **ngoài git** (pattern `**/service-account*.json` đã có sẵn trong `.gitignore`
từ trước), mount vào container qua volume trong `docker-compose.prod.yml`, set
`APP_FIREBASE_ENABLED=true` và `APP_FIREBASE_SERVICE_ACCOUNT_PATH=/path/trong/container.json`.

⚠️ **Lưu ý quan trọng**: kể cả bật Firebase thật, outbox worker hiện **chưa gửi được push nào**
vì chưa có bảng đăng ký device token của merchant (tính năng đó thuộc `merchant_app`, Stage 2).
`FirebaseFcmSender` đã sẵn sàng, chỉ chờ Stage 2 nối `deviceToken` thật vào lời gọi
`fcmSender.send(...)` ở `OutboxBatchProcessor`. Không phải bug — cấu trúc đã đúng, chỉ chưa có
consumer.

**→ Khi có file service-account:** set `APP_FIREBASE_ENABLED=true` +
`APP_FIREBASE_SERVICE_ACCOUNT_PATH=...` trong `.env.prod`, mount file vào container qua volume
trong `docker-compose.prod.yml` (agent thêm dòng volume), redeploy, kiểm tra log KHÔNG còn dòng
`[fcm] ... LogOnlyFcmSender` mà thấy `[fcm] Firebase khởi tạo thành công`. Đây là điều kiện #3
trong "Tiêu chí đóng Stage 1". **Đừng tự dựng bảng device token/route đăng ký FCM token** — đó là
việc của `merchant_app`, Stage 2, chưa bàn.

## 4. R2 (Cloudflare) — object storage ảnh, phía backend

Property: `app.storage.mode` (`local` mặc định | `s3`), và nhóm `app.storage.s3.*`: `endpoint`,
`bucket`, `access-key`, `secret-key`, `region` (R2 dùng `"auto"`), `public-base-url`.

Cần: tạo Cloudflare R2 bucket, bật "Public access" (hoặc gắn custom domain) để ảnh đọc được công
khai qua HTTP không auth — xem ghi chú trong test `S3ImageStorageServiceTest` (bucket MinIO test
cũng phải set policy public-read tương tự, phản ánh đúng bước cần làm ở R2), tạo API token
(access key + secret key), lấy endpoint dạng `https://<account_id>.r2.cloudflarestorage.com`.

⚠️ Cũng như Firebase: **chưa có endpoint HTTP upload ảnh nào gọi tới** — tính năng chụp ảnh món ăn
là `merchant_app` Stage 2. R2 credential chỉ cần điền khi Stage 2 thật sự cần upload.

**→ Khi có credential R2:** chỉ cần set `app.storage.mode=s3` + 5 property `app.storage.s3.*`
trong `.env.prod`/`docker-compose.prod.yml` để SẴN SÀNG — **không có việc gì để "verify" thêm**
vì chưa có endpoint upload nào gọi tới (không phải điều kiện đóng Stage 1). Việc HTTP upload +
resize thật sự thuộc Stage 2, làm khi đến lúc, không làm sớm (xem cảnh báo PM ở đầu file).

## 5. Backup Postgres

`infra/docs/README.md` đã ghi "diễn tập khôi phục backup một lần trước khi có shop thật" — **job
backup tự động CHƯA được làm** ở lượt này (ngoài phạm vi Stage 1 theo `docs/70-stages.md`, thuộc
Stage 3 "Vận hành"). Cần: chọn nơi lưu backup (object storage khác, hoặc chính R2 ở mục 4), tần
suất, và một lần diễn tập restore thật trước khi có dữ liệu shop thật.

**→ Không phải việc của Stage 1** — thuộc Stage 3 "Vận hành" (`docs/70-stages.md`). Agent phiên
sau: đừng tự dựng job backup khi chưa được giao rõ, kể cả nếu mục 1-4 đã xong hết. Đây là mục
duy nhất trong file này KHÔNG nằm trong "Tiêu chí đóng Stage 1" — chỉ ghi lại để không quên.

## 6. Firebase Crashlytics — phía customer_app (client)

**Đã nối code (2026-08-22), CHỈ còn thiếu file config thật.** `firebase_core`/`firebase_crashlytics`
đã thêm vào `pubspec.yaml`, `CrashReporting.init()` (`lib/config/crash_reporting.dart`) đã gọi ở cả
3 flavor (`main_dev.dart`/`main_staging.dart`/`main_prod.dart`). Plugin Gradle
`com.google.gms.google-services`/`com.google.firebase.crashlytics` chỉ tự apply khi tìm thấy file
config thật (`android/app/build.gradle.kts`, biến `hasGoogleServicesConfig`) — **hiện chưa có file
nào nên plugin không apply, build Android không đổi so với trước** (đã verify:
`flutter build apk --flavor dev -t lib/main_dev.dart --debug` build thành công 2026-08-22).

Cần chủ dự án làm để bật thật:
1. Tạo Firebase project (dùng chung với mục 3 hoặc project riêng).
2. Thêm app Android cho từng flavor thật cần Crashlytics (package name theo `applicationIdSuffix`
   — vd `vn.dynamicshop.customer_app.dev` cho flavor dev), tải `google-services.json`.
3. Đặt file vào `apps/customer_app/android/app/src/<flavor>/google-services.json` (đã có sẵn
   trong `.gitignore` — pattern `**/google-services.json` — không lo commit nhầm).
4. Build lại — plugin tự kích hoạt, Crashlytics bắt đầu ghi nhận crash thật.
5. iOS (`GoogleService-Info.plist`) — **chưa verify được** ở lượt này (máy dev là Windows, không
   build được iOS). Cần làm + build thử trên macOS trước khi build release iOS thật.

⚠️ Việc build/verify Android ở trên là compile-check tối thiểu (`flutter pub get` +
`flutter analyze` + 1 lần `flutter build apk --debug`), KHÔNG phải `flutter test` hay verify trên
thiết bị thật — phần đó vẫn thuộc phạm vi chủ dự án tự làm theo quy trình đã chốt (`progress.md`
mục 7). Xem `aware.md` để biết vì sao build-check được coi là cần thiết ở lượt sửa Gradle này.

**→ Khi có `google-services.json` đặt đúng chỗ:** agent chỉ cần build thử (`flutter build apk
--flavor <flavor> --debug`) để xác nhận plugin Gradle tự kích hoạt không lỗi — **không chạy
`flutter test`, không cài lên máy/emulator để verify Crashlytics ghi nhận crash thật**, đó là việc
chủ dự án tự làm (`progress.md` mục 7, quy trình FE/mobile). Đây là điều kiện #2/#3 gián tiếp
trong "Tiêu chí đóng Stage 1" (customer_app cần chạy được, Firebase cần thật).
