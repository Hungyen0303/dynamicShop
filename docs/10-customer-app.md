# 10 — Customer App

Đọc kèm `00-context.md` và **`70-stages.md`**. Doc này ghi **ràng buộc**, không mô tả code.

**Stage 0** — customer_app là một trong hai thứ được làm bây giờ (cùng backend).

---

## Bất biến

1. **Layout, theme, menu, giá tải lúc RUNTIME.** Không nhồi vào binary — nếu không, sửa một giá món là phải build lại và submit store.
2. **Package dùng chung không hardcode style.** `ds_blocks`, `ds_components` chỉ đọc từ `DsTokens`. CI có lint chặn.
3. **Block lạ không được crash.** Type không có trong registry → `SizedBox.shrink()` + telemetry.
4. **Mọi field trong config đều optional, có default.** Không `as double`, không `!`.
5. **Không SDUI cho giỏ hàng, checkout, thanh toán, đăng nhập, theo dõi đơn.** Hardcode.
6. **Một round-trip cho storefront.** Block không tự gọi API riêng.
7. **Không có màn hình trắng.** Luôn có fallback (xem mục Fallback).

---

## Flavor — mô hình bán đứt

Quyết định 08/2026: mô hình kinh doanh là **bán đứt app riêng cho từng shop**, nên **một shop = một flavor** là chấp nhận được.

### Nhưng: flavor chỉ chứa DANH TÍNH, không chứa NỘI DUNG

Đây là ranh giới quan trọng nhất của cả file này.

| Thuộc về flavor (build-time) | Thuộc về runtime config |
|---|---|
| Tên app, icon, splash | Menu, giá, ảnh món |
| `applicationId` / bundle id | Theme (màu, font, bo góc) |
| `DEFAULT_TENANT_ID` | Layout, thứ tự block |
| Firebase config file | Giờ mở cửa, khuyến mãi |

**Vì sao phải tách:** nếu theme và menu nằm trong binary, chủ quán đổi giá một món là bạn phải build lại + submit store + chờ duyệt 1–7 ngày. Với 100 shop thì không sống nổi. Runtime config giữ cho việc rebuild chỉ xảy ra khi **sửa code**, không phải khi shop đổi nội dung.

### Một codebase, N flavor sinh tự động

**Không** copy 100 lần code. Giữ một `customer_app`, sinh cấu hình flavor từ file khai báo:

```yaml
# flavors/quan-bun-co-ba.yaml
tenant_id: 8f14e45f-...
app_name: "Bún Cô Ba"
application_id: vn.dynamicshop.quanbuncoba
icon: assets/shops/co-ba/icon.png
firebase_config: secrets/co-ba/google-services.json
```

Script sinh ra `productFlavors` (Android) và scheme (iOS) từ thư mục `flavors/`. Thêm shop = thêm một file YAML, không sửa code.

### Ba flavor môi trường luôn tồn tại
`dev`, `staging`, `prod` — dùng khi phát triển và test, không gắn với shop nào.

### Cảnh báo vận hành cần biết trước
- **Publish dưới developer account của chính shop.** Nhiều app template giống nhau trên cùng một account sẽ bị Apple từ chối theo Guideline 4.3.
- Sửa một bug = rebuild + resubmit **mọi** flavor đang bán. Đây là lý do **bảo trì và cập nhật được tính phí riêng** — chi phí đó phải được chuyển cho khách hàng, nếu không mô hình lỗ.
- Giữ số lượng flavor cần rebuild ở mức tối thiểu: mọi thứ sửa được bằng runtime config thì đừng đưa vào flavor.

---

## Nguồn sự thật

| Thứ | File |
|---|---|
| Danh sách block, props, whitelist override | `contracts/blocks.registry.json` |
| Cấu trúc JSON storefront | `contracts/storefront.schema.json` |
| Design token | `contracts/tokens.json` → sinh `ds_tokens` |
| API | `contracts/openapi.yaml` → sinh `ds_api` |

**Không sửa `packages/ds_tokens/lib/generated/` hay `packages/ds_api/lib/generated/` bằng tay.**

---

## SDUI — mức 2, không hơn

Storefront = **danh sách block phẳng** + theme tokens. Không lồng nhau, không Row/Column trong JSON, không biểu thức logic trong JSON.

```json
{
  "schema_version": 3,
  "theme": { "primary": "#E23744", "radius_md": 12, "font_family": "Be Vietnam Pro" },
  "screens": {
    "home": {
      "blocks": [
        { "id": "hero_1", "type": "hero_banner", "v": 1,
          "props": { "image_url": "…", "aspect_ratio": 2.0 } },
        { "id": "grid_1", "type": "product_grid", "v": 2,
          "props": { "columns": 2, "card_style": "elevated" },
          "data_ref": "products:cat_do_uong" }
      ]
    }
  }
}
```

**Layout tách khỏi data.** Block khai báo `data_ref`, server resolve sẵn và trả cùng response:

```
GET /v1/s/{slug}/storefront?schema=3
→ { "layout": {…}, "data": { "products:cat_do_uong": [...], "categories": [...] } }
```

### Chuỗi phân giải style

```
giá trị = blockOverride ?? variantPreset ?? tenantTheme ?? appDefault
```

Áp dụng cho **mọi** thuộc tính, qua `StyleResolver`. Không widget nào tự quyết thứ tự riêng.

### Variant là preset, không phải widget class

Sai: `MinimalProductCard`, `ElevatedProductCard`, `BorderedProductCard`…
Đúng: một `ProductCard` + `ProductCardStyle.presets['elevated']`.

Thêm variant = thêm một entry trong map, không thêm file.

### Màu chữ tự suy, không cho cấu hình

```dart
Color onColorOf(Color bg) =>
    bg.computeLuminance() > 0.5 ? const Color(0xFF1A1A1A) : Colors.white;
```

Shop chọn màu nền, màu chữ tự tính. Ngăn chủ quán tạo ra chữ trắng trên nền vàng nhạt.

---

## Registry — hai chốt chặn bắt buộc

```dart
Widget build(BuildContext ctx, BlockNode node, BlockScope scope) {
  final builder = _builders[node.type];

  // (1) forward compat
  if (builder == null) {
    Telemetry.unknownBlock(node.type, node.v);
    return const SizedBox.shrink();
  }

  // (2) error boundary
  try {
    return builder(ctx, node, scope);
  } catch (e, st) {
    Telemetry.blockRenderError(node.id, node.type, e, st);
    return const SizedBox.shrink();
  }
}
```

Không được bỏ hoặc "đơn giản hoá" hai khối này. `(1)` là điều kiện để thêm block mới mà không phá app cũ. `(2)` là điều kiện để một shop cấu hình sai không làm trắng màn hình.

---

## Fallback ba tầng

```
1. Server layout (fresh)                    ─ lỗi/parse fail ─┐
2. Cached layout (disk, key theo tenant+schema)  ←────────────┤
3. assets/default_storefront.json                ←────────────┘
```

Thêm: Firebase Remote Config `sdui_enabled = false` → bỏ qua tầng 1+2, dùng tầng 3. Đây là kill switch khi push nhầm schema lỗi.

**Test bắt buộc:** chặn API và xác nhận app vẫn hiển thị được storefront.

---

## Parse JSON — luôn khoan dung

```dart
extension Json on Map<String, dynamic> {
  String? str(String k) => this[k] is String ? this[k] as String : null;
  double? dbl(String k) => (this[k] as num?)?.toDouble();  // nhận cả int
  int?    integer(String k) => (this[k] as num?)?.toInt();
  bool    bl(String k, {bool or = false}) => this[k] is bool ? this[k] as bool : or;
}
```

Lý do cụ thể: thêm field mới ⇒ config của N shop đã lưu trong DB không có field đó ⇒ parser nghiêm khắc làm N shop trắng màn hình cùng lúc.

---

## Mạng yếu — 3G tỉnh là mặc định

- Mở app → render cache **ngay**, fetch nền, chỉ update nếu ETag đổi
- **Không spinner toàn màn hình** khi đã có cache
- Timeout 10s, retry exponential backoff
- Ảnh: URL phải trỏ CDN đã resize, không phải ảnh gốc
- Giỏ hàng lưu local, không mất khi mất mạng
- Tạo đơn **luôn gửi kèm `Idempotency-Key`** (UUID client sinh, giữ nguyên khi retry)

---

## Font — cái bẫy tiếng Việt

Nhiều font không đủ dấu tiếng Việt. Chữ sẽ hiện sai và bạn chỉ biết khi shop gửi ảnh chụp màn hình.

- Danh sách cho phép nằm trong `contracts/tokens.json` → `allowed_fonts`. **Không nhận tên font tự do.**
- Bundle 2–3 font phổ biến nhất, còn lại `google_fonts` tải runtime + cache
- **Preload font của tenant trước frame đầu tiên** — nếu không sẽ nháy đổi font

---

## Công thức cho task hay gặp

### Thêm một block type mới
> ⚠️ Cần người duyệt trước (xem `AGENTS.md` mục 7).

1. Thêm entry vào `contracts/blocks.registry.json` (type, props + default, whitelist override)
2. `make generate`
3. Tạo widget trong `packages/ds_blocks/lib/blocks/` — pure, không network, chỉ đọc token
4. Đăng ký trong `ds_sdui` registry
5. Thêm fixture vào `test/fixtures/layouts/` + golden test
6. Kiểm tra `studio_web` render đúng (dùng chung package, thường tự chạy)
7. `make verify-contracts && make test`

### Thêm một token mới
1. Thêm vào `contracts/tokens.json` **kèm default**
2. `make generate` → cập nhật `DsTokens` (Dart) và `tokens.ts` (web)
3. Kiểm tra parse cũ không vỡ: config không có field mới vẫn phải chạy

### Sửa một block đang có
1. Kiểm tra ai đang dùng: `customer_app` **và** `studio_web`
2. Nếu đổi/xoá prop → tăng `v` của block trong registry, giữ tương thích ngược
3. Cập nhật golden test

---

## Test bắt buộc

| Loại | Nội dung |
|---|---|
| Golden | Mọi fixture layout trong `test/fixtures/layouts/` |
| Coverage | Mọi block đã đăng ký phải có ít nhất 1 fixture |
| Resilience | Layout chứa block type lạ → `returnsNormally` |
| Fallback | Chặn API → vẫn render được từ bundled |
| Lint | Không có `Colors.` / `Color(0x` / `BorderRadius.circular(<số>)` trong package dùng chung |

Chi tiết ở `docs/50-qa.md`.

---

## Không làm

- Không thêm block mới nếu chưa có ≥3 shop yêu cầu (hỏi PM)
- Không cho phép biểu thức/điều kiện trong JSON layout
- Không cho block lồng block
- Không mở style tự do — chỉ token và whitelist
- Không import `dart:io` hoặc code chỉ chạy trên mobile vào `ds_blocks` (studio_web build web)
