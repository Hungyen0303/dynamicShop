# 20 — Web: Next.js Admin + Flutter Studio

> 🔴 **Next.js admin = STAGE 3.**
> 🔴 **Flutter studio = GẦN NHƯ CHẮC CHẮN KHÔNG LÀM** — merchant app đã đảm nhận toàn bộ vai trò studio (kéo thả block, chỉnh style, xem trước trực tiếp), ngay trên điện thoại. Xem `11-merchant-app.md`.
> Xem `70-stages.md`. Nếu được giao task thuộc file này, dừng lại và xác nhận với người trước.

Đọc kèm `00-context.md` và `70-stages.md`.

---

## Đây là công cụ nội bộ, không phải sản phẩm cho khách

Người dùng chính là **operator** (nhân sự DynamicShop và CTV), không phải chủ shop. Chủ shop ở tỉnh không vào web; họ dùng merchant app.

Hệ quả cho mọi quyết định thiết kế:

| | Nếu cho chủ quán | Thực tế: cho operator |
|---|---|---|
| Ưu tiên | Dễ dùng cho người không rành | **Nhanh** — dựng 20 shop một tối |
| Cần | Onboarding, tooltip | Template, **duplicate từ shop khác**, phím tắt, thao tác hàng loạt |
| Đẹp? | Bắt buộc | Không ưu tiên |

**Tính năng quan trọng nhất của studio không phải drag-drop, mà là nhân bản storefront.** Ưu tiên nó trước.

---

## Bất biến

1. **Auth admin tách hoàn toàn khỏi merchant.** Subdomain riêng, JWT `audience` khác, bảng user khác. Không có đường nào từ token merchant lên quyền admin.
2. **2FA bắt buộc** cho mọi tài khoản admin. Không cho tắt.
3. **Impersonate luôn ghi audit log** + hiện banner cảnh báo rõ ràng trên UI.
4. **`postMessage` luôn kiểm tra `event.origin`.** Không bao giờ dùng `'*'`.
5. **Studio không giữ state.** Next.js giữ toàn bộ state; Flutter chỉ render.
6. **Flutter web bundle lazy-load.** Chỉ tải khi vào `/studio`.
7. **Zod schema layout sinh từ `contracts/storefront.schema.json`**, không viết tay bản thứ hai.

---

## Cấu trúc

```
ops.dynamicshop.vn  (Next.js App Router)
├── /                    6 chỉ số sống còn
├── /tenants             danh sách shop
├── /tenants/[id]        chi tiết, đổi gói, impersonate
├── /studio/[id]         ← iframe Flutter web
└── /billing             trạng thái thanh toán
```

---

## Quan hệ với customer app — điểm quan trọng nhất của doc này

```
apps/studio_web (Flutter web)
      ↓ import
packages/ds_sdui + ds_blocks + ds_tokens
      ↑ import
apps/customer_app (Flutter mobile)
```

**Cùng `BlockRegistry`, cùng widget code.** WYSIWYG được đảm bảo về mặt cấu trúc, không phải bằng kỷ luật.

⚠️ **Hệ quả bắt buộc nhớ:** sửa một block trong `ds_blocks` là sửa **cả hai** bề mặt. Trước khi merge, kiểm tra ảnh hưởng hai chiều.

⚠️ **`ds_blocks` phải build được cho web.** Không import `dart:io`, không dùng plugin chỉ có trên mobile. Nếu cần, tách qua conditional import.

### Nếu ai đó đề xuất dựng lại block bằng React

Từ chối. Preview sẽ lệch dần khỏi app thật, chủ quán mất niềm tin, và mỗi thay đổi block phải làm hai lần. Đây là lý do duy nhất `studio_web` tồn tại bằng Flutter.

---

## Cầu nối Next.js ↔ Flutter

```ts
// Next.js → Flutter (debounce ~200ms mỗi khi state đổi)
iframe.contentWindow.postMessage(
  { type: 'layout_update', payload: draftLayout }, TRUSTED_ORIGIN);

// Flutter → Next.js
window.parent.postMessage(
  { type: 'block_selected', id: 'grid_1' }, TRUSTED_ORIGIN);
```

Message types được định nghĩa ở `contracts/studio-bridge.schema.json`. Thêm message type mới ⇒ sửa contract trước.

**Preview dùng data thật của shop, không dùng mock.** Chủ quán cần thấy đúng món của mình, và nó lộ ra lỗi kiểu "tên món quá dài bị tràn" ngay trong studio.

---

## Panel chỉnh sinh tự động

`contracts/blocks.registry.json` đã khai báo thuộc tính nào được override cho block nào. **Panel bên phải sinh tự động từ đó.**

Không viết UI riêng cho từng loại block. Nếu một block cần UI đặc biệt, đó là dấu hiệu nên sửa registry chứ không phải thêm case đặc biệt.

---

## Trang chủ = 6 chỉ số sống còn

Đây không phải dashboard chung chung. Đúng 6 số này, đúng thứ tự này:

| Chỉ số | Ngưỡng cảnh báo |
|---|---|
| % shop có ≥1 đơn ở tuần thứ 4 | <30% → đỏ |
| Số đơn/shop/tuần | hiển thị phân phối, không chỉ trung bình |
| Retention tháng 3 | <60% → đỏ |
| GMV | xu hướng 12 tuần |
| CAC vs LTV | CAC > 1/3 LTV → đỏ |
| Time-to-first-order | >24h → đỏ |

Định nghĩa tính toán nằm ở `contracts/metrics.json` để BE và web dùng chung một công thức. **Không tính lại ở phía web.**

---

## Bảo mật

| Hệ | Đăng nhập | Phạm vi |
|---|---|---|
| customer | Zalo / OTP | không quyền quản trị |
| merchant | SĐT + mật khẩu, gắn `tenant_id` | chỉ tenant của mình |
| **admin** | tài khoản riêng + **TOTP 2FA** | **mọi** tenant |

- Subdomain riêng, JWT `audience` riêng, session ngắn, refresh token xoay vòng
- Giai đoạn đầu: giới hạn IP hoặc VPN
- Admin gọi BE qua DataSource có `BYPASSRLS` (xem `31-database.md`) — chỉ dùng ở service admin

---

## Công nghệ

| Thứ | Dùng | Ghi chú |
|---|---|---|
| Framework | Next.js App Router + TypeScript | Server Components cho trang đọc, Client cho editor |
| Bảng | TanStack Table | **Server-side pagination ngay từ đầu** |
| Biểu đồ | Recharts | Không dùng thư viện chart nặng |
| Data fetching | TanStack Query | Query key luôn gắn `tenantId` |
| Validate | Zod, sinh từ contract | Không viết schema tay |
| 2FA | `otplib` | |
| Studio | Flutter web (CanvasKit) | Bundle ~2MB+, lazy-load |
| Deploy | Vercel **hoặc** cùng VPS (Next standalone + Caddy) | |

---

## Công thức cho task hay gặp

### Thêm một cột vào bảng tenant
1. Kiểm tra API đã trả field đó chưa (`contracts/openapi.yaml`)
2. Nếu chưa → sửa contract, `make generate`, sửa BE trước
3. Thêm column definition, giữ server-side pagination

### Thêm message type mới cho studio bridge
1. Sửa `contracts/studio-bridge.schema.json`
2. `make generate` → type TS + model Dart
3. Xử lý ở cả hai đầu, luôn kiểm tra `origin`

### Sửa cách hiển thị một block trong studio
Không sửa trong `studio_web`. Sửa trong `packages/ds_blocks` — nếu không, preview sẽ lệch app thật.

---

## Test

| Loại | Nội dung |
|---|---|
| Unit | Công thức 6 chỉ số khớp `contracts/metrics.json` |
| Integration | Token merchant **không** truy cập được endpoint admin |
| Integration | Impersonate sinh đúng bản ghi audit log |
| E2E (Playwright) | Đăng nhập + 2FA → tạo tenant → dựng storefront → preview khớp |
| Visual | Ảnh chụp preview studio vs golden của `customer_app` cùng layout |

Test cuối cùng là chốt chặn chống drift giữa studio và app.

---

## Không làm

- Không dựng lại block bằng React
- Không tính lại chỉ số ở web — dùng API
- Không lưu state layout trong Flutter
- Không hiển thị chỉ số của một tenant trên trang tổng hợp mà không qua kiểm tra quyền
- Không làm module hoa hồng CTV lúc này (đã hoãn — hỏi PM trước khi bắt đầu)
