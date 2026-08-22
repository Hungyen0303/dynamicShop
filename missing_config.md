# missing_config.md — Cấu hình thật còn thiếu (Stage 1 treo, Stage 2 đang mock quanh nó)

> Mọi thứ dưới đây đã có **fallback an toàn**: không điền gì thì app vẫn chạy đầy đủ ở local (đã
> verify `./gradlew clean test` **64/64 xanh** sau sprint 2.1b + chạy thật `docker compose ... up`).
> Danh sách này là việc **chủ dự án tự làm sau** khi có tài khoản/thiết bị thật — nó **không chặn**
> việc dựng code Stage 2 (xem `progress.md` mục 10: thiếu gì thì mock).
> Xem `progress.md` mục 9 + 10 để biết phạm vi và luật mock, `aware.md` để biết quyết định kỹ thuật
> nào cần tự check lại (mọi mock đều được ghi ở đó).

## 🔴 Trạng thái (2026-08-22, cập nhật lần 3) — đọc trước khi làm bất cứ mục nào

**Chủ dự án đã quyết: ĐI TIẾP STAGE 2 và LÀM HẾT các phần còn lại, 6 mục dưới đây cứ để treo.**
Thiếu credential thì **mock và ghi lại**, KHÔNG dừng chờ. Xem `progress.md` **mục 9** (phạm vi
sprint) và **mục 10** (luật mock — đọc trước khi viết code). Nghĩa là **hai stage mở cùng lúc**:
Stage 1 treo chờ credential, Stage 2 làm tới hết bằng mock ở chỗ nào thiếu.

Mọi mục dưới đây đều cần chủ dự án tự đi lấy (tài khoản, thanh toán, xác minh danh tính), không
phải việc code. Agent phiên sau: **đừng tự đi "làm thử" hay đoán giá trị cho các mục này**, nhưng
cũng **đừng lấy chúng làm lý do dừng** — chỉ dùng file này để biết mục nào đã điền, mục nào còn
trống, việc kế tiếp là gì cho mục đó (mỗi mục có ghi "→ agent làm gì tiếp khi có").

Đánh dấu `[x]` khi chủ dự án đã điền xong một mục, để agent sau biết mục nào sẵn sàng làm tiếp:

- [ ] 1. VPS + domain thật
- [ ] 2. Secrets `.env.prod` thật (đổi khỏi giá trị mẫu)
- [ ] 3. Firebase service-account thật (backend FCM)
- [ ] 4. Cloudflare R2 credential thật
- [ ] 5. Kế hoạch backup Postgres
- [ ] 6. Firebase project + `google-services.json` (customer_app **và** merchant_app)

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

## 🟡 Stage 2 khi thiếu credential — MOCK CÁI GÌ, CHỜ XÁC MINH CÁI GÌ (cập nhật 2026-08-22 lần 3)

Ghi ở đây thay vì tạo file trạng thái thứ ba. Mục này từng có tiêu đề "việc Stage 2 bị CHẶN" —
**cách gọi đó sai và đã bỏ**: chủ dự án chốt làm hết Stage 2, thiếu gì mock nấy (`progress.md` mục
10). Phân biệt cho rõ hai loại, vì chúng đòi hỏi hành động khác hẳn nhau:

### A. Mock được → LÀM LUÔN, không chờ (kèm luật mock ở `progress.md` mục 10)

| Thiếu | Mock | Ghi ở đâu khi làm xong |
|---|---|---|
| Device token FCM thật | `PushTokenProvider` interface + `FakeTokenProvider` (token giả cố định, chỉ ở flavor dev) | `aware.md` |
| Push tới máy | Không mock — dùng kênh **polling thật**, một `IncomingOrderSink` duy nhất | `aware.md` |
| Crashlytics merchant_app | Khuôn guard y hệt `customer_app`: plugin Gradle chỉ apply khi có `google-services.json` thật | `aware.md` |
| R2 | **Không cần mock** — `LocalDiskImageStorageService` là bản thật, chạy được (`app.storage.mode=local`) | — |
| Server thật/domain | Backend local, app trỏ `localhost` hoặc IP LAN qua cấu hình flavor (không hardcode) | `aware.md` |

### B. KHÔNG mock được → chờ xác minh, nhưng KHÔNG chặn việc dựng code

Ba thứ này là **phép đo trên phần cứng/mạng thật**, không phải hạng mục code. Mock chúng không tạo
ra thông tin mới, chỉ tạo niềm tin sai:

- **Push đánh thức máy đang ngủ / bị OEM kill** — kịch bản test 1/2/6/7 trong
  `docs/11-merchant-app.md`. Câu hỏi về hành vi MIUI/ColorOS + Doze, không phải về code của mình.
- **Latency `orders/sync` qua 4G thật** — cần mục 1 (VPS + domain).
- **Chuông còn kêu sau 8 tiếng chạy nền** — chạy được **sớm bằng polling-only** ngay cuối sprint
  2.2, không cần đợi FCM. Tốn *thời gian đồng hồ*, không tốn công.

### C. Chủ dự án cần chuẩn bị cho bài test 8 tiếng (có lead time — bắt đầu sớm)

- 🔴 **Máy Xiaomi hoặc Oppo thật.** Samsung SM-A256E đã dùng ở Stage 0 vẫn nên chạy, nhưng
  MIUI/ColorOS mới là thứ giết app nền. Đi mượn/mua máy cũ sớm.
- 🔴 **Backend truy cập được từ điện thoại.** `adb reverse` KHÔNG dùng được vì nó chết khi rút cáp,
  mà bài test này **bắt buộc rút cáp** (máy phải chạy pin để Android vào Doze; cắm sạc thì test
  xanh giả). Hai lựa chọn:
  1. **Laptop + Wi-Fi LAN** (làm được ngay, không tốn tiền): backend chạy trên laptop, flavor dev
     của merchant_app trỏ `http://192.168.x.x:8080`, cho phép cleartext ở flavor dev, mở firewall
     Windows cổng 8080, và **tắt sleep của laptop suốt 8 tiếng**.
  2. **Lấy VPS ở mục 1** — sạch hơn hẳn và mở khoá luôn phép đo qua 4G thật.
- **Cách bắn đơn lúc 3 giờ sáng mà không đụng vào máy** — script `curl POST /v1/s/{slug}/orders`
  kèm `Idempotency-Key` khác nhau, hẹn giờ 30 phút/lần bằng Task Scheduler. Không có nó thì bài
  test chỉ chứng minh "app còn sống", không chứng minh "chuông còn kêu".

**→ Hệ quả, nói thẳng:** sau khi làm hết 2.2–2.4, **code Stage 2 xong**, nhưng **bar hoàn thành
Stage 2** (`docs/70-stages.md`: "chạy nền 8 tiếng trên Xiaomi/Oppo thật, gửi đơn lúc 3 giờ sáng,
chuông vẫn kêu") **vẫn chưa đạt** — vì đó là bài kiểm tra vật lý, không phải hạng mục code. Lúc đó
phần còn lại là **xác minh**, không phải **xây dựng**. Agent phiên sau đọc thấy "Stage 2 chưa đóng"
thì đừng tưởng còn thiếu code.

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

⚠️ **Cập nhật sau sprint 2.1** — phần thiếu đã thu hẹp lại: bảng `device_tokens` + route đăng ký
(`POST/DELETE /v1/merchant/devices`) **đã có**, và `OutboxBatchProcessor` **đã fan-out** tới mọi máy
còn sống của đúng tenant (có test chống rò rỉ chéo tenant). Nghĩa là toàn bộ đường ống "đơn mới →
outbox → worker → resolve token đúng tenant → gọi sender" đã chạy thật.

Thứ duy nhất còn thiếu là **chính file service-account này** — thiếu nó thì `FcmSender` vẫn là
`LogOnlyFcmSender`, đường ống chạy đủ nhưng dừng ở dòng log thay vì gọi Google. Điền file vào là
push thật chạy, không phải viết thêm code backend nào.

**→ Khi có file service-account:** (đây giờ là mục có giá trị cao nhất trong cả file — nó mở khoá
push thật cho merchant_app) set `APP_FIREBASE_ENABLED=true` +
`APP_FIREBASE_SERVICE_ACCOUNT_PATH=...` trong `.env.prod`, mount file vào container qua volume
trong `docker-compose.prod.yml` (agent thêm dòng volume), redeploy, kiểm tra log KHÔNG còn dòng
`[fcm] ... LogOnlyFcmSender` mà thấy `[fcm] Firebase khởi tạo thành công`. Đây là điều kiện #3
trong "Tiêu chí đóng Stage 1". ~~**Đừng tự dựng bảng device token/route đăng ký FCM token**~~ — đã làm xong ở sprint 2.1.

## 4. R2 (Cloudflare) — object storage ảnh, phía backend

Property: `app.storage.mode` (`local` mặc định | `s3`), và nhóm `app.storage.s3.*`: `endpoint`,
`bucket`, `access-key`, `secret-key`, `region` (R2 dùng `"auto"`), `public-base-url`.

Cần: tạo Cloudflare R2 bucket, bật "Public access" (hoặc gắn custom domain) để ảnh đọc được công
khai qua HTTP không auth — xem ghi chú trong test `S3ImageStorageServiceTest` (bucket MinIO test
cũng phải set policy public-read tương tự, phản ánh đúng bước cần làm ở R2), tạo API token
(access key + secret key), lấy endpoint dạng `https://<account_id>.r2.cloudflarestorage.com`.

⚠️ **Cập nhật 2026-08-22 (lần 3): mục này KHÔNG chặn sprint 2.3.** `LocalDiskImageStorageService`
(`app.storage.mode=local`, mặc định) là bản **thật**, không phải mock — upload + resize sẽ được dựng
và test đầy đủ trên nó. R2 chỉ là đổi nơi lưu file, không đổi một dòng logic nào ở tầng trên, vì mọi
thứ đi qua interface `ImageStorageService`.

**→ Khi có credential R2:** set `app.storage.mode=s3` + 5 property `app.storage.s3.*` trong
`.env.prod`/`docker-compose.prod.yml`. Sau sprint 2.3 thì **có việc để verify thật**: gọi endpoint
upload, kiểm ảnh đọc được công khai qua `public-base-url` không cần auth (đây chính là chỗ bucket
MinIO từng trả 403 lúc test — xem `S3ImageStorageServiceTest`). Chạy test upload trên **cả hai**
mode `local` và MinIO trước khi tin là R2 sẽ chạy.

## 5. Backup Postgres

`infra/docs/README.md` đã ghi "diễn tập khôi phục backup một lần trước khi có shop thật" — **job
backup tự động CHƯA được làm** ở lượt này (ngoài phạm vi Stage 1 theo `docs/70-stages.md`, thuộc
Stage 3 "Vận hành"). Cần: chọn nơi lưu backup (object storage khác, hoặc chính R2 ở mục 4), tần
suất, và một lần diễn tập restore thật trước khi có dữ liệu shop thật.

**→ Không phải việc của Stage 1** — thuộc Stage 3 "Vận hành" (`docs/70-stages.md`). Agent phiên
sau: đừng tự dựng job backup khi chưa được giao rõ, kể cả nếu mục 1-4 đã xong hết. Đây là mục
duy nhất trong file này KHÔNG nằm trong "Tiêu chí đóng Stage 1" — chỉ ghi lại để không quên.

## 6. Firebase Crashlytics — phía client (customer_app, và merchant_app từ sprint 2.2)

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

⚠️ **Từ sprint 2.2, mục này còn thêm một vai trò quan trọng hơn nhiều**: `merchant_app` cần chính
file `google-services.json` này để `getToken()` của FCM trả về **device token thật**. Cho tới lúc
đó merchant_app dùng `FakeTokenProvider` (token giả cố định, chỉ flavor dev) — route đăng ký device
vẫn chạy end-to-end thật, chỉ là token không dùng được để gửi push. Đây là mục có giá trị mở khoá
cao nhất trong cả file, cùng với mục 3.
