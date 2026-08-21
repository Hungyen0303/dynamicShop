# 03 — Web: Next.js Admin + Flutter Studio

*Tech spec — DynamicShop, 07/2026*

---

## Nhận định định hình toàn bộ phần này

Chủ shop ở tỉnh **không vào web**. Họ sống trên điện thoại.

Hệ quả — và đây là điểm dễ bỏ qua: nếu chủ shop không dùng studio, vậy **ai** ngồi cấu hình storefront?

Nhìn kế hoạch GTM: tối hôm trước dựng sẵn 20 storefront để sáng hôm sau đi bán. Người dùng studio ở đó **là bạn**. Sau này là CTV.

**Studio không phải sản phẩm cho khách hàng — nó là công cụ vận hành nội bộ.**

Điều đó thay đổi hoàn toàn mục tiêu thiết kế:

| | Nếu cho chủ quán | Thực tế: cho operator |
|---|---|---|
| Ưu tiên | Dễ tới mức cô bán bún dùng được | **Nhanh** — 20 shop một tối |
| Cần | Onboarding, tooltip, hand-holding | Template, **duplicate từ shop khác**, phím tắt, thao tác hàng loạt |
| Đẹp? | Bắt buộc | Xấu cũng được |

Tính năng quan trọng nhất của studio **không phải drag-drop**, mà là *"nhân bản storefront của quán bún kia sang quán này rồi đổi logo"*.

---

## Một web, hai phần

```
ops.dynamicshop.vn  (Next.js)
├── /                    6 chỉ số sống còn
├── /tenants             quản lý shop
├── /tenants/[id]        chi tiết, đổi gói, xem-với-tư-cách
├── /studio/[id]         ← nhúng Flutter web (iframe)
└── /billing             trạng thái thanh toán
```

**Vì sao nhúng thay vì hai app riêng:** operator vốn sống trong admin cả ngày. Một lần đăng nhập, một domain, một lần deploy. Flutter chỉ làm đúng việc nó giỏi duy nhất — render preview chính xác pixel.

---

## Vì sao studio phải là Flutter, không phải React

Nếu dựng lại block bằng React, preview sẽ **lệch dần** so với app thật. Chủ quán chỉnh xong, mở app lên thấy khác → mất niềm tin, và bạn phải sửa hai codebase mỗi khi đổi một block.

```
apps/studio_web (Flutter web)
      ↓ import
packages/ds_sdui + ds_blocks + ds_tokens
      ↑ import
apps/customer_app (Flutter mobile)
```

Cùng `BlockRegistry`, cùng widget code. **WYSIWYG được đảm bảo về mặt cấu trúc, không phải bằng kỷ luật.**

```dart
DeviceFrame(
  device: DeviceInfo.genericPhone(),
  child: StorefrontRenderer(
    layout: editorState.draftLayout,
    data: previewData,          // ← data THẬT của shop, không phải mock
    theme: editorState.draftTheme,
  ),
)
```

Dùng data thật quan trọng: chủ quán cần thấy đúng món của mình, và nó lộ ra lỗi kiểu "tên món quá dài bị tràn" ngay trong studio.

### Cầu nối Next.js ↔ Flutter

```js
// Next.js → Flutter (mỗi lần state đổi, debounce ~200ms)
iframe.contentWindow.postMessage(
  { type: 'layout_update', payload: draftLayout }, TRUSTED_ORIGIN);

// Flutter → Next.js
window.parent.postMessage(
  { type: 'block_selected', id: 'grid_1' }, TRUSTED_ORIGIN);
```

Next.js giữ **toàn bộ** state, form, undo/redo, lưu nháp. Flutter chỉ render — không giữ state, không gọi API.

⚠️ Luôn kiểm tra `event.origin`, không dùng `'*'`.

### Panel chỉnh sinh tự động

Vì `kOverridable` (xem file 01) đã khai báo tường minh thuộc tính nào được override cho block nào, panel bên phải **tự sinh từ bảng đó**. Không cần thiết kế UI riêng cho từng loại block — đó là phần thưởng của việc khai báo tường minh.

---

## Trang chủ admin = 6 chỉ số sống còn

Đây là công cụ của riêng bạn, nên đừng làm dashboard chung chung. Làm đúng thứ bạn cần để ra quyết định mỗi sáng:

| Chỉ số | Ngưỡng |
|---|---|
| **% shop có ≥1 đơn ở tuần thứ 4** | >50%. Dưới 30% → sửa sản phẩm, đừng tuyển sales |
| **Số đơn/shop/tuần** | Tìm ra **ngưỡng ma thuật** của riêng bạn |
| **Retention tháng 3** | <60% → **tuyệt đối không scale** |
| **GMV** | Theo dõi xu hướng |
| **CAC vs LTV** | CAC < 1/3 LTV |
| **Time-to-first-order** | <24h |

Đây là chỗ trả nợ cho lời khuyên *"lập file theo dõi từ shop đầu tiên"* — biến nó thành trang bạn mở mỗi sáng.

**Về chỉ số số 2:** khi phát hiện *"shop vượt 15 đơn/tuần thì retention 90%, dưới thì 40%"*, toàn bộ roadmap sản phẩm có một mục tiêu duy nhất — đẩy shop qua ngưỡng đó. Không còn phải đoán nên làm tính năng gì. Đây là thứ giá trị nhất admin có thể cho bạn.

---

## Các module còn lại

**Quản lý tenant** — tạo shop, đổi gói, tạm ngưng, trạng thái thanh toán, ngày onboard.

**Xem-với-tư-cách-shop (impersonate)** — để debug khi chủ quán gọi *"app em bị lỗi"*. Cực kỳ hữu ích.

> ⚠️ **Bắt buộc có audit log**: ai xem tenant nào, lúc nào, bao lâu. Bạn đang nắm dữ liệu khách hàng của người ta; một ngày nào đó sẽ có người hỏi. Hiện banner đỏ rõ ràng khi đang ở chế độ impersonate để không nhầm.

**Hoa hồng CTV** — *tạm hoãn theo quyết định hiện tại.* Khi làm, luật là: chỉ trả khi shop đạt ≥20 đơn thật trong 30 ngày đầu, không trả theo lượt đăng ký.

---

## 🔒 Bảo mật: giờ có 3 hệ auth, đừng trộn

| Hệ | Đăng nhập | Phạm vi |
|---|---|---|
| Customer | Zalo login / OTP | Không có quyền quản trị |
| Merchant | SĐT + mật khẩu, gắn `tenant_id` | Chỉ tenant của mình |
| **Internal admin** | Tài khoản riêng + **2FA** | **Mọi** tenant |

**Bốn việc phải làm:**

1. **Subdomain riêng** — `ops.dynamicshop.vn`, JWT khác `audience`
2. **Token merchant không bao giờ escalate được thành admin** — đây là lỗi phổ biến khi dùng chung một bảng `users`. Tách bảng, tách issuer.
3. **Giới hạn IP hoặc VPN** ở giai đoạn đầu — rẻ, và tránh được rủi ro lớn nhất
4. **Admin dùng DataSource riêng** với DB role `BYPASSRLS` (xem file 04) — tách vật lý thì không thể vô tình dùng nhầm

---

## Công nghệ tích hợp

| Công nghệ | Why | How |
|---|---|---|
| **Next.js (App Router) + TypeScript** | Form/bảng dựng nhanh hơn Flutter web nhiều; SSR cho landing sau này | Server Components cho trang đọc, Client cho editor |
| **Auth.js / tự viết JWT** | Auth admin tách hoàn toàn khỏi tenant | Audience riêng, session ngắn, refresh có xoay vòng |
| **TOTP 2FA** | Admin thấy dữ liệu mọi shop | `otplib`, bắt buộc không cho tắt |
| **TanStack Table** | Bảng tenant/đơn cần sort, filter, phân trang | Server-side pagination ngay từ đầu |
| **Recharts** | 6 chỉ số cần biểu đồ xu hướng đơn giản | Đừng dùng thư viện chart nặng |
| **TanStack Query** | Cache, refetch, optimistic update | Query key gắn `tenantId` |
| **Zod** | Validate payload layout trước khi lưu | **Dùng chung schema với validate phía BE** — sinh từ một nguồn |
| **Flutter web (CanvasKit)** | Preview pixel-perfect | Build ra `web/`, serve như static asset cùng domain |
| **Sentry** | Lỗi phía admin | Chung project với BE để trace xuyên tầng |
| **Vercel *hoặc* cùng VPS** | Deploy đơn giản | Nếu VPS: Next.js standalone + Caddy, cùng docker-compose với BE |

**Lưu ý CanvasKit:** bundle Flutter web nặng (~2MB+). Chấp nhận được vì đây là công cụ nội bộ trên desktop, nhưng nhớ lazy-load — chỉ tải khi vào `/studio`, không tải ở trang chủ.

---

## Hoãn studio lại — 10 shop đầu không cần

Với 10 shop đầu tiên, **seed script hoặc sửa JSON trực tiếp là đủ và nhanh hơn**:

```bash
./seed-storefront.sh --tenant=quan-bun-co-ba \
  --template=fnb-basic --logo=./logos/co-ba.png --primary="#E23744"
```

Studio chỉ thực sự cần khi mạng CTV chạy (tuần 9–12). Ưu tiên merchant app trước.

Nhưng **trang 6 chỉ số nên có sớm** — bạn cần nó từ shop đầu tiên, và nó rẻ hơn studio nhiều.

---

## Thứ tự build

| Sprint | Việc |
|---|---|
| 5–6 | Next.js skeleton + auth admin + 2FA + **trang 6 chỉ số** + quản lý tenant |
| 7 | Impersonate + audit log |
| 7–8 | Flutter studio nhúng — **ưu tiên số 1 là chức năng nhân bản**, drag-drop sau |
| Sau | Hoa hồng CTV (khi bật lại), billing |

---

## Checklist

- [ ] Admin ở subdomain riêng, JWT audience khác merchant
- [ ] 2FA bắt buộc, không cho tắt
- [ ] Không có đường nào từ token merchant lên quyền admin (viết test)
- [ ] Impersonate ghi audit log + banner cảnh báo rõ
- [ ] `postMessage` kiểm tra `origin`, không dùng `'*'`
- [ ] Studio bundle lazy-load, không tải ở trang chủ
- [ ] Preview trong studio khớp 100% app thật (so ảnh chụp màn hình)
- [ ] Zod schema layout dùng chung với BE, không viết hai bản
