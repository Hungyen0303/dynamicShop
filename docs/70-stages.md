# 70 — Giai đoạn (Stages)

**Agent đọc file này trước khi bắt đầu BẤT KỲ task nào.** Nó quyết định thứ gì được phép làm bây giờ và thứ gì phải để sau.

---

## Nguyên tắc: local trước, mọi thứ khác sau

> **Không kết nối gì ra ngoài cho tới khi flow chạy thông suốt trên máy local với 2 mock shop.**

Lý do: mỗi dịch vụ ngoài (Firebase, store, máy in, CI) thêm một biến số. Debug một hệ thống có 6 biến số cùng lúc là cách chắc chắn nhất để mất một tháng mà không hiểu gì.

Mục tiêu Stage 0 không phải "có sản phẩm" mà là **hiểu được toàn bộ flow bằng tay mình**.

---

## Stage 0 — Local hoàn chỉnh 🟢 ĐANG Ở ĐÂY

**Điều kiện hoàn thành:** mở máy lên, chạy vài lệnh, và thấy được **toàn bộ vòng đời một đơn hàng** với 2 shop giả lập — không cần internet.

### Trong phạm vi

| Thành phần | Chạy ở đâu |
|---|---|
| Postgres | Docker local |
| Spring Boot | `./gradlew bootRun` local |
| customer_app | Emulator / máy thật, trỏ về `localhost` |
| Dữ liệu 2 mock shop | SQL fixture, nạp bằng Flyway |

### Flow phải chạy được đầu-cuối

```
1. Mở customer_app, chọn mock shop A
2. Storefront hiện đúng theme + menu của shop A
3. Đổi sang mock shop B → theme và menu khác hẳn
4. Thêm món vào giỏ, đặt hàng
5. Đơn xuất hiện trong DB, order_events có dòng PENDING
6. Gọi API xác nhận đơn (Postman/curl) → trạng thái đổi, order_events thêm dòng
7. Gửi lại đúng request đặt hàng đó → KHÔNG tạo đơn thứ hai (idempotency)
8. Query DB với tenant B → KHÔNG thấy đơn của tenant A
```

Bước 3, 7, 8 là ba bước quan trọng nhất — chúng chứng minh multi-tenant, SDUI, và idempotency thực sự hoạt động.

### ❌ Ngoài phạm vi Stage 0 — không được tự ý làm

| Việc | Lý do hoãn |
|---|---|
| **In bill nhiệt** | Cần máy in thật; không liên quan tới việc hiểu flow |
| **CI/CD** | Chưa có gì để deploy |
| **Seed script** | 2 mock shop dùng SQL fixture là đủ; script hàng loạt cần khi đi bán |
| **Melos** | Dùng `path:` dependency trực tiếp trong `pubspec.yaml`. Melos khi có nhiều package thật. |
| **merchant_app** | Stage 2 |
| **Firebase / FCM** | Stage 1 |
| **Next.js admin** | Stage 3 |
| **studio_web** | Stage 4 (có thể không bao giờ) |
| **Zalo (login, OA, ZNS)** | Hoãn vô thời hạn theo quyết định của chủ dự án |
| **Đối soát thanh toán tự động** | Nút thủ công là đủ |
| **VPS, domain, deploy** | Stage 3 |

### Auth ở Stage 0
Đơn giản nhất có thể: JWT tự cấp, đăng nhập bằng số điện thoại + mật khẩu cứng trong fixture. **Không** OTP, **không** Zalo, **không** provider ngoài. Hai mặt phẳng (public/authenticated) vẫn phải đúng — đó là kiến trúc, không phải tính năng.

### Ảnh ở Stage 0
Lưu vào thư mục local, Spring serve tĩnh. Không R2, không CDN.

---

## Stage 1 — Hạ tầng ảo + Firebase

**Bắt đầu khi:** Stage 0 xong hết, bạn giải thích được flow cho người khác nghe.

- Dựng VPS, deploy Postgres + Spring Boot bằng Docker Compose
- Domain + TLS (Caddy)
- Firebase project: FCM, Crashlytics
- customer_app trỏ về server thật
- Object storage cho ảnh (R2) + resize lúc upload

**Điều kiện hoàn thành:** customer_app trên máy thật, dùng mạng 4G, đặt được đơn lên server thật.

---

## Stage 2 — Merchant app

**Bắt đầu khi:** Stage 1 xong, FCM gửi/nhận được ổn định.

- merchant_app Android: nhận đơn + FCM + foreground service + chuông
- Xử lý OEM giết app nền (xem `11-merchant-app.md`)
- Offline queue
- **Chụp ảnh / quay video món ăn và post trực tiếp từ điện thoại** ← tính năng cốt lõi
- **Cấu hình giao diện đầy đủ trên điện thoại**: kéo thả thứ tự block, ẩn/hiện, chọn style, chỉnh bo góc + màu nền từng block, đổi theme và font, kèm xem trước trực tiếp ← merchant app LÀ studio
- Báo cáo cơ bản + push tổng kết cuối ngày
- Nút "Đã nhận tiền"

⚠️ **merchant_app cần `ds_blocks` + `ds_sdui`** (khác thiết kế ban đầu) — xem trước trực tiếp phải dùng đúng `renderStorefront()` của customer_app, nếu không sẽ lệch.

**Chưa làm ở Stage 2:** in bill nhiệt (Stage 5).

**Điều kiện hoàn thành:** chạy nền 8 tiếng trên máy Xiaomi/Oppo thật, gửi đơn lúc 3 giờ sáng, chuông vẫn kêu.

---

## Stage 3 — Vận hành

- Next.js admin (**tối giản**): quản lý tenant, 6 chỉ số, quota dung lượng
- Tính năng **export dữ liệu** cho shop (xem mô hình bán đứt bên dưới)
- CI/CD
- Backup tự động + diễn tập restore

---

## Stage 4 — Mở rộng (có thể không cần)

- Seed script hàng loạt
- Melos (khi package đủ nhiều)
- ~~studio_web~~ — merchant app đã đảm nhận; chỉ dựng nếu cần cấu hình hàng loạt cho nhiều shop
- CRM khách quen

---

## Stage 5 — Phần cứng & tích hợp

- In bill nhiệt K80/K58
- Tích hợp Ahamove / Grab Express
- Đối soát thanh toán tự động (SePay)

---

## Bảng tra nhanh cho agent

Trước khi bắt đầu, tra việc mình định làm ở đây:

| Việc | Stage | Được làm bây giờ? |
|---|---|---|
| Schema, RLS, test cô lập tenant | 0 | ✅ |
| Storefront API, catalog API | 0 | ✅ |
| Order + state machine + order_events | 0 | ✅ |
| Idempotency | 0 | ✅ |
| customer_app + SDUI render | 0 | ✅ |
| Mock data 2 shop | 0 | ✅ |
| Outbox (bảng + worker, chưa nối FCM) | 0 | ✅ ghi log thay vì gửi |
| Deploy, VPS, domain | 1 | ❌ |
| Firebase, FCM | 1 | ❌ |
| merchant_app | 2 | ❌ |
| Next.js admin | 3 | ❌ |
| Export dữ liệu + quota | 3 | ❌ |
| CI/CD | 3 | ❌ |
| Seed script, melos | 4 | ❌ |
| studio_web | — | ❌ **gần như chắc chắn không làm** |
| In bill nhiệt | 5 | ❌ |
| Zalo bất cứ thứ gì | — | ❌ **hoãn vô thời hạn** |

**Nếu một task nằm ở stage sau: dừng lại, nói rõ nó thuộc stage nào, hỏi người có muốn làm sớm không.** Đừng tự ý nhảy stage.
