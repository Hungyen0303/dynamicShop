# 60 — Design System

Doc này định nghĩa **ràng buộc thị giác**. Agent đọc khi task đụng UI, token, hoặc component.

> 🟢 **Stage 0:** chỉ áp cho `customer_app`. Ràng buộc merchant_app là Stage 2, admin là Stage 3.

---

## Nguồn sự thật

```
contracts/tokens.json   →  make generate  →  packages/ds_tokens/  (Dart)
                                          →  web/admin/tokens.ts  (TS)
```

**Không sửa file sinh ra bằng tay.** Không khai báo màu/spacing ở nơi khác.

---

## Bất biến

1. **Không hardcode màu, bo góc, spacing** trong `ds_blocks` và `ds_components`. CI có lint chặn.
2. **Component đọc token từ `context`**, không nhận màu qua constructor.
3. **Màu chữ tự suy từ màu nền**, không cho cấu hình riêng.
4. **Font chỉ chọn từ danh sách cho phép** trong `tokens.json`. Không nhận tên font tự do.
5. **Variant là preset trong map**, không phải widget class riêng.
6. **Mọi token có default.** Config thiếu field vẫn phải render được.

---

## Token

```json
{
  "colors": {
    "primary":    { "default": "#E23744", "configurable": true },
    "surface":    { "default": "#FFFFFF", "configurable": true },
    "on_surface": { "default": "#1A1A1A", "configurable": false },
    "border":     { "default": "#E5E5E5", "configurable": false },
    "danger":     { "default": "#D32F2F", "configurable": false },
    "success":    { "default": "#2E7D32", "configurable": false }
  },
  "radius":  { "sm": 6, "md": 12, "lg": 20 },
  "spacing": { "unit": 8 },
  "typography": {
    "font_family": { "default": "Be Vietnam Pro", "configurable": true },
    "scale": { "default": "compact", "options": ["compact", "comfortable"] }
  },
  "allowed_fonts": [
    "Be Vietnam Pro", "Inter", "Roboto", "Nunito Sans",
    "Lexend", "Open Sans", "Montserrat", "Quicksand"
  ]
}
```

`configurable: false` nghĩa là shop **không** được đổi. Studio không hiện ô chỉnh cho những token đó.

**Spacing luôn là bội số của `unit` (8).** Không có `padding: 13`.

---

## Chuỗi phân giải — áp cho mọi thuộc tính

```
giá trị = blockOverride ?? variantPreset ?? tenantTheme ?? appDefault
```

```dart
class StyleResolver {
  double get radius =>
      blockOverride.dbl('radius')   // 1. khách chỉnh riêng block này
   ?? variant.radius                 // 2. preset của style đã chọn
   ?? theme.radiusMd                 // 3. theme của shop
   ?? DsDefaults.radiusMd;           // 4. mặc định app
}
```

Không widget nào tự quyết thứ tự riêng. Đây là nguồn gốc của bug "shop đổi theme mà nút không đổi màu".

---

## Component đọc token, không nhận màu

```dart
// ✅
class DsButton extends StatelessWidget {
  @override
  Widget build(BuildContext ctx) {
    final t = Theme.of(ctx).extension<DsTokens>()!;
    return Material(
      color: t.primary,
      borderRadius: BorderRadius.circular(t.radiusMd),
      child: ...,
    );
  }
}

// ☠️
class DsButton extends StatelessWidget {
  final Color color;              // không — mở đường cho hardcode ở call site
  const DsButton({required this.color});
}
```

Dùng `ThemeExtension<DsTokens>` của Flutter, **không tự viết `InheritedWidget`**. `lerp()` phải implement để animation đổi theme mượt.

---

## Màu chữ tự suy — một dòng chặn cả một loại bug

```dart
Color onColorOf(Color bg) =>
    bg.computeLuminance() > 0.5 ? const Color(0xFF1A1A1A) : Colors.white;
```

Chủ quán sẽ chọn `primary` = vàng nhạt, và chữ trắng biến mất. Cho họ chọn màu nền, **không** cho chọn màu chữ.

Đây là lý do `on_surface` có `configurable: false`.

---

## Variant = preset, không phải class

```dart
class ProductCardStyle {
  final double? radius;
  final double elevation;
  final bool showBorder;
  final bool overlayText;
  const ProductCardStyle({this.radius, this.elevation = 0,
                          this.showBorder = false, this.overlayText = false});

  static const presets = <String, ProductCardStyle>{
    'minimal':  ProductCardStyle(radius: 0,  elevation: 0, showBorder: false),
    'elevated': ProductCardStyle(radius: 12, elevation: 2),
    'bordered': ProductCardStyle(radius: 8,  showBorder: true),
    'overlay':  ProductCardStyle(radius: 16, overlayText: true),
  };
}
```

Một widget, N preset. Thêm variant thứ 5 = thêm **một dòng**, không thêm file.

Nếu mỗi variant là widget riêng: 4 variant × 8 block = 32 widget phải maintain.

---

## Whitelist thuộc tính override

Nguồn: `contracts/blocks.registry.json`.

```json
{
  "product_grid": { "overridable": ["radius", "bg", "card_style", "columns"] },
  "hero_banner":  { "overridable": ["radius", "aspect_ratio"] },
  "promo_strip":  { "overridable": ["bg"] },
  "category_row": { "overridable": ["style"] },
  "spacer":       { "overridable": ["height"] }
}
```

Mỗi entry kèm loại control (`slider` / `color_token` / `segmented` / `toggle`) + min/max/label.

Ba lý do phải khai báo tường minh:
1. Refactor block sau này không làm hỏng shop đang chạy
2. **Màn hình cấu hình trong MERCHANT APP sinh tự động từ bảng này** — không thiết kế UI riêng cho từng block
3. Không có whitelist thì tổ hợp cần test là vô hạn

Chủ quán chỉnh trên điện thoại, không qua web.

`promo_strip` chỉ cho đổi `bg`, không cho `fg` — vì màu chữ tự suy.

---

## Font — cái bẫy tiếng Việt

Rất nhiều font đẹp **không có đủ dấu tiếng Việt**. Chữ hiện ra kiểu "Trà s?a" hoặc dấu bị lệch, và bạn chỉ phát hiện khi shop gửi ảnh chụp màn hình.

- Chỉ dùng font trong `allowed_fonts`. **Thêm font mới phải kiểm tra bộ dấu đầy đủ trước.**
- Bundle 2–3 font phổ biến nhất vào app; còn lại `google_fonts` tải runtime + cache
- **Preload font của tenant trước frame đầu tiên** — nếu không sẽ nháy đổi font rất xấu
- Chuỗi test dấu: `Trà sữa trân châu đường đen — Phở bò tái nạm gầu`

---

## Ràng buộc thiết kế theo bề mặt

| Bề mặt | Ưu tiên | Ghi chú |
|---|---|---|
| **customer_app** | Đẹp, nhanh, brand của shop nổi bật | Brand DynamicShop **không** được nổi hơn brand shop |
| **merchant_app** | To, rõ, bấm được khi tay bận | Không cần đẹp. Nút ≥48dp, chữ ≥16sp, tương phản cao. **Cũng là nơi cấu hình giao diện storefront.** |
| **admin + studio** | Dày đặc thông tin, thao tác nhanh | Công cụ nội bộ. Không cần đẹp. |

### Merchant app — ràng buộc cụ thể

Môi trường: quán ăn 7 giờ tối, ồn, sáng chói, tay dính dầu mỡ, nhìn lướt qua.

- Vùng chạm tối thiểu **48dp**, ưu tiên 56dp cho hành động chính
- Chữ tối thiểu **16sp**, số liệu quan trọng ≥24sp
- Tương phản tối thiểu **4.5:1** (WCAG AA) — màn hình có thể đang bị nắng chiếu
- Trạng thái đơn phân biệt bằng **màu + hình dạng + chữ**, không chỉ màu
- **Không animation** trên luồng nhận đơn — mỗi mili-giây đều tính

---

## Ảnh

- Chủ quán sẽ upload ảnh 8MB chụp từ điện thoại
- **URL trong storefront luôn trỏ CDN đã resize**, không bao giờ ảnh gốc
- Tỉ lệ chuẩn: card sản phẩm 1:1, banner 2:1, hero 16:9
- Luôn có placeholder — mạng 3G, ảnh tải chậm là mặc định

---

## Công thức cho task hay gặp

### Thêm một token
1. Thêm vào `contracts/tokens.json` **kèm `default`**
2. `make generate`
3. Kiểm tra config cũ (không có field mới) vẫn parse được
4. Nếu `configurable: true` → kiểm tra studio hiện ô chỉnh đúng

### Thêm một component vào `ds_components`
1. Chỉ đọc token từ `context`, không nhận màu qua constructor
2. Không hardcode gì — lint sẽ chặn
3. Phải build được cho **web** (studio dùng chung) — không `dart:io`
4. Thêm golden test

### Đổi màu mặc định
Sửa `default` trong `tokens.json`. Không tìm-thay trong code.

---

## Không làm

- Không thêm màu ngoài token
- Không dùng spacing lẻ (không bội số 8)
- Không cho shop cấu hình màu chữ
- Không nhận tên font tự do
- Không tạo widget riêng cho mỗi variant
- Không import `dart:io` hoặc plugin chỉ có trên mobile vào package dùng chung
