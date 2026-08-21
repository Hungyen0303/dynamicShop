# 11 — Merchant App

> 🔴 **STAGE 2 — CHƯA LÀM BÂY GIỜ.**
> Bắt đầu sau khi Stage 1 xong (VPS + Firebase + FCM chạy ổn định). Xem `70-stages.md`.
> Nếu được giao task thuộc file này, dừng lại và xác nhận với người trước.

Đọc kèm `00-context.md` và `70-stages.md`. Doc này ghi **ràng buộc**, không mô tả code.

---

## Mục tiêu duy nhất

**Không bao giờ sót đơn.**

App này không cần đẹp. Nó cần chạy nền 8 tiếng trên máy Xiaomi và vẫn kêu chuông lúc 3 giờ sáng. Mọi đánh đổi đều nghiêng về độ tin cậy.

Chủ shop mở app ~50 lần/ngày, khách mở customer app ~2 lần/tuần. Chất lượng app này **là** retention của cả công ty.

---

## Bất biến

1. **Hai kênh nhận đơn, không tin vào một cái nào.** FCM (background) + polling 15–20s (foreground).
2. **Không dùng WebSocket.** Đã cân nhắc và loại — 3G tỉnh rớt liên tục.
3. **Mọi hành động đổi trạng thái đi qua offline queue** kèm `Idempotency-Key`. Không gọi API trực tiếp từ UI.
4. **Push là at-least-once** → luôn dedupe theo `order_id`.
5. **Merchant app LÀ studio.** Cấu hình giao diện đầy đủ: thứ tự block, ẩn/hiện, style, radius, bg, theme, font.
6. **CẦN `ds_blocks` + `ds_sdui`** để xem trước trực tiếp khi chỉnh giao diện — dùng đúng `renderStorefront()` mà customer_app dùng.
7. **Platform channel giới hạn 4.** Thêm cái thứ 5 cần người duyệt.
8. **Ngày kinh doanh ≠ ngày lịch.** Luôn dùng `tenant.business_day_start` từ server, không tự tính theo nửa đêm.

---

## Nền tảng

**Android trước. iOS sau hoặc không.**

Lý do kỹ thuật: iOS không đảm bảo được chuông báo đơn khi app ở background. Critical Alerts cần entitlement đặc biệt của Apple, rất khó xin. Gần như toàn bộ chủ quán ở tỉnh dùng Android.

Không viết code iOS cho luồng nhận đơn cho tới khi có quyết định ngược lại từ PM.

---

## 🔴 OEM giết app nền — vấn đề số 1

Xiaomi (MIUI), Oppo (ColorOS), Vivo, Realme, Samsung có lớp quản lý pin riêng, mạnh tay hơn Android gốc. Chúng kill foreground service, chặn FCM, không cho autostart sau khi khởi động máy.

**Không xuất hiện trên Pixel hay emulator.** Bạn sẽ không phát hiện cho tới khi chủ quán gọi điện.

Bốn thứ bắt buộc phải có:

| # | Việc | Ghi chú |
|---|---|---|
| 1 | Xin `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Dialog ngay trong onboarding, không để sau |
| 2 | Hướng dẫn autostart theo hãng | Detect `Build.MANUFACTURER` → mở thẳng màn hình cài đặt tương ứng + ảnh minh hoạ |
| 3 | Foreground service + notification thường trực | "Đang nhận đơn" — cái giá phải trả để sống sót |
| 4 | **Màn hình tự kiểm tra** | "Trạng thái nhận đơn: ✅ Bình thường / ⚠️ Có thể bị chặn" |

Mục 2 và 4 là tính năng chính, không phải việc phụ. Chúng tách app dùng được khỏi app bị bỏ.

---

## Kiến trúc nhận đơn

| Trạng thái | Cơ chế |
|---|---|
| Background / khoá màn hình | FCM **data message** → đánh thức foreground service → chuông |
| Foreground | Polling `GET /v1/merchant/orders/sync?since=…` mỗi 15–20s |
| Mở lại sau khi mất mạng | Full sync theo `updated_at` |

Dùng `data` payload, **không** dùng `notification` payload — để tự kiểm soát cách hiển thị và âm thanh.

### Chuông phải khó bỏ qua

Quán lúc 7 giờ tối rất ồn. Chuông mặc định sẽ bị bỏ lỡ.

- **Lặp cho tới khi có người bấm xác nhận.** Không kêu một lần.
- Dùng stream **ALARM**, không phải NOTIFICATION (ALARM không bị chế độ im lặng tắt)
- Full-screen intent để hiện đè lên màn hình khoá
- Rung kèm
- Channel `new_order` với `importance = MAX`
- Cho chủ quán chọn âm thanh và **test ngay trong cài đặt**

---

## Offline queue

```
Bấm "Xác nhận đơn"
  → ghi vào SQLite queue (drift)
  → cập nhật UI ngay (optimistic)
  → worker gửi lên server, retry backoff
  → có mạng lại: flush theo thứ tự
```

- Mọi action mang `Idempotency-Key` do client sinh → server dedupe → retry an toàn
- **Hiển thị rõ trạng thái đồng bộ**: "3 thao tác đang chờ gửi". Đừng giấu.
- Queue phải sống sót qua việc app bị kill → lưu SQLite, không lưu memory

---

## ⭐ Đăng nội dung từ điện thoại — tính năng cốt lõi Stage 2

Chủ quán **không ngồi laptop**. Họ chụp ảnh món ăn hôm đó hoặc quay video ngay tại quán và muốn đăng lên luôn. Toàn bộ việc quản lý nội dung nằm ở đây, không nằm ở web.

Phải có:
- Chụp ảnh trực tiếp trong app, hoặc chọn từ thư viện
- **Nén + resize ngay trên máy trước khi upload** — chủ quán chụp ảnh 8MB, mạng 3G không tải nổi
- Quay/chọn video ngắn, nén trước khi gửi
- Upload chạy nền, có thể tắt app — dùng cùng cơ chế queue với đơn hàng
- Gắn ảnh vào món, đổi ảnh banner, đăng "món hôm nay"
- Thấy rõ tiến độ upload và dung lượng còn lại của quota

Đây là lý do `studio_web` bị hạ ưu tiên xuống Stage 4 và có thể không bao giờ làm.

---

## In bill

> ⚠️ **STAGE 5 — hoãn.** Cần máy in thật để test. Không làm ở Stage 2.

**Thử package sẵn trước khi viết platform channel.**

| Thư viện | Vai trò |
|---|---|
| `esc_pos_utils` | Sinh byte lệnh ESC/POS |
| `flutter_pos_printer_platform` / `blue_thermal_printer` | Truyền Bluetooth / USB |

**Bắt buộc lưu ý:**

- **Tiếng Việt có dấu**: máy in nhiệt Trung Quốc thường không có codepage tiếng Việt → **render bill thành bitmap rồi in ảnh**. Chậm hơn ~200ms nhưng luôn đúng dấu. Đây là cách làm mặc định, không phải phương án dự phòng.
- **Khổ giấy cấu hình được**: K80 = 80mm ≈ 48 ký tự/dòng; K58 = 58mm ≈ 32 ký tự. Không hardcode.
- **Tự động reconnect** — máy in mất kết nối là chuyện thường ngày
- **Nút "In lại"** trên mọi đơn
- **Hàng đợi in** khi máy in bận, không bỏ lệnh

---

## Báo cáo — thiết kế cho điện thoại, không phải dashboard

Không bê biểu đồ web xuống. Với chủ quán ở tỉnh:

**Một con số lớn + một so sánh:**
```
Hôm nay
1.240.000đ  ·  47 đơn
↑ 12% so với thứ Ba tuần trước
```

**Ba dòng, không phải ba biểu đồ:** món bán chạy nhất, giờ đông nhất, số khách quay lại.

Cộng sổ thu chi cuối ngày.

### Push tổng kết cuối ngày — ưu tiên cao

> "Tối nay quán mình bán 47 đơn, 1.240.000đ. Món chạy nhất: bún bò đặc biệt."

Chủ quán không cần mở app vẫn thấy giá trị mỗi ngày. Chi phí gần bằng 0, tác động lớn. Làm sớm, có thể trước cả màn hình báo cáo đầy đủ.

Gửi tại `business_day_start` của tenant, không phải nửa đêm.

---

## ⭐ Cấu hình giao diện — merchant app LÀ studio

Chủ quán làm **toàn bộ** việc cấu hình giao diện ở đây, trên điện thoại. Không cần web.

### Chủ quán đổi được gì

| Việc | Cơ chế |
|---|---|
| **Đổi thứ tự block** | Kéo thả (`ReorderableListView`) |
| **Ẩn/hiện block** | Công tắc |
| **Chọn style block** | Segmented control từ `variantPreset` |
| **Chỉnh bo góc từng block** | Slider → `blockOverride.radius` |
| **Chỉnh màu nền từng block** | Bảng màu → `blockOverride.bg` |
| **Đổi theme** | Màu chủ đạo, bo góc chung |
| **Đổi font** | Chọn từ `allowed_fonts` |
| **Nội dung** | Ảnh bìa, chữ khuyến mãi, món, giá |

Đây là chuỗi phân giải đã thống nhất — chủ quán điều khiển ba trong bốn tầng:

```
giá trị = blockOverride ?? variantPreset ?? tenantTheme ?? appDefault
             ▲                ▲               ▲
             │                │               └─ đổi màu chủ đạo
             │                └───────────────── chọn style block
             └────────────────────────────────── chỉnh riêng block này
```

Component **không** có màn hình chỉnh riêng, nhưng đổi gián tiếp: chỉnh `radius` của một block → block truyền xuống `DsCard(radius:)` bên trong nó.

### Ba màn hình

**Danh sách block — kéo thả, bật/tắt**
```
┌────────────────────────────────┐
│  ← Giao diện quán        [Lưu] │
├────────────────────────────────┤
│  Giữ và kéo để đổi thứ tự      │
│  ⣿  Ảnh bìa            [ON ]  ⚙│
│  ⣿  Danh mục món       [ON ]  ⚙│
│  ⣿  Lưới món ăn        [ON ]  ⚙│
│  ⣿  Dải khuyến mãi     [OFF]  ⚙│
│         [+ Thêm phần]          │
└────────────────────────────────┘
```

**Chỉnh một block — bấm ⚙, có xem trước trực tiếp**
```
┌────────────────────────────────┐
│  ← Lưới món ăn                 │
│  ┌──────────────────────────┐  │
│  │  [ xem trước trực tiếp ]  │  │
│  └──────────────────────────┘  │
│  Kiểu thẻ món                  │
│  [Đơn giản][Nổi▲][Viền][Đè]    │
│  Số cột       ( )2   (•)3      │
│  Bo góc    ────────●──── 16    │
│  Màu nền   ⬜🟥🟧🟨🟩🟦🟪 [+]   │
│  [Đặt lại về mặc định]         │
└────────────────────────────────┘
```

**Màu sắc & phông chữ** — màu chủ đạo, font (từ danh sách), bo góc chung, xem trước cả trang.

### 🔴 Màn hình ⚙ SINH TỰ ĐỘNG, không viết tay

`contracts/blocks.registry.json` khai báo mỗi thuộc tính kèm loại control:

```json
"product_grid": {
  "overridable": {
    "card_style": { "control": "segmented", "options": [...], "label": "Kiểu thẻ món" },
    "columns":    { "control": "segmented", "options": [2,3], "label": "Số cột" },
    "radius":     { "control": "slider", "min": 0, "max": 24, "step": 2, "label": "Bo góc" },
    "bg":         { "control": "color_token", "label": "Màu nền" }
  }
}
```

Merchant app đọc bảng này và dựng form. **Thêm một entry vào registry = màn hình tự có thêm ô.** Không có `if (blockType == 'product_grid')` ở bất kỳ đâu.

Nếu bạn thấy mình đang viết UI riêng cho một loại block, dừng lại — thứ cần sửa là registry.

### ⚠️ Hệ quả kiến trúc: merchant app CẦN `ds_blocks` + `ds_sdui`

Vì có xem trước trực tiếp, merchant app phải render block thật bằng đúng `renderStorefront()` mà customer_app dùng.

```
merchant_app: ds_tokens ✅  ds_components ✅  ds_blocks ✅  ds_sdui ✅
```

Đây là thay đổi so với thiết kế trước (từng ghi "không import ds_sdui"). Nếu render bằng code riêng, xem trước sẽ lệch app khách — đúng lỗi mà cả kiến trúc package này sinh ra để tránh.

Đổi lại: **merchant app chính là studio**, nên `studio_web` gần như chắc chắn không cần tồn tại.

### Không cho chỉnh — whitelist bảo vệ chủ quán

- **Màu chữ**: tự suy từ luminance của nền. Chủ quán chọn nền vàng nhạt + chữ trắng = chữ biến mất.
- **Font tự do**: chỉ chọn từ `allowed_fonts` — nhiều font không đủ dấu tiếng Việt.
- **Thuộc tính ngoài whitelist**: mỗi block chỉ mở đúng những gì khai báo trong registry.

### Template khởi tạo

Lúc bán app, chọn template từ `contracts/templates/` → có layout hợp lý ngay. Chủ quán tự do sửa sau đó. Luôn có nút **"Đặt lại về template"**.

---

## Công thức cho task hay gặp

### Thêm một hành động đổi trạng thái đơn
1. Kiểm tra transition có hợp lệ trong `contracts/order-states.json` không
2. Thêm vào offline queue với action type mới
3. Sinh `Idempotency-Key`
4. Optimistic update UI + rollback nếu server từ chối
5. Test: tắt mạng giữa chừng → action phải vào queue và tự gửi lại

### Thêm platform channel
> ⚠️ Cần người duyệt. Hiện có 4, đây là trần.

1. Xác nhận không có package Flutter nào làm được
2. Viết cả phía Android; **iOS trả `UnimplementedError` rõ ràng**, không silently no-op
3. Wrap trong một abstraction ở Dart để test được bằng fake

---

## Test bắt buộc

Đây là app mà unit test không đủ. Bắt buộc có **kiểm thử trên máy thật**:

| # | Kịch bản | Bar |
|---|---|---|
| 1 | Chạy nền 8 tiếng trên Xiaomi và Oppo thật → gửi đơn | Chuông vẫn kêu |
| 2 | Khởi động lại điện thoại | App tự chạy lại, vẫn nhận đơn |
| 3 | Tắt Wi-Fi giữa lúc xác nhận đơn | Action vào queue, tự gửi khi có mạng |
| 4 | Máy in tắt rồi bật | Tự reconnect, in lại được |
| 5 | In bill trên máy in thật | Đúng dấu tiếng Việt |
| 6 | Gửi push trùng 2 lần | Chỉ một bản ghi đơn |
| 7 | Bật chế độ im lặng | Chuông vẫn kêu (stream ALARM) |

Không đánh dấu "xong" một tính năng nhận đơn nếu chưa chạy kịch bản 1.

---

## Không làm

- Không dùng `shared_prefs` cho queue hoặc dữ liệu đơn — dùng `drift`
- Không gọi API trực tiếp từ widget, luôn qua queue/repository
- Không thêm animation/hiệu ứng làm chậm luồng nhận đơn
- Không dùng `notification` payload của FCM
- Không tự tính "hôm nay" theo nửa đêm
