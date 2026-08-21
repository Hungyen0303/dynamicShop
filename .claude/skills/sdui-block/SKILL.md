---
name: sdui-block
description: Dùng khi làm việc với server-driven UI của storefront — thêm hoặc sửa block, chỉnh registry, xử lý layout JSON, theme token, style resolver, hoặc fallback. Áp dụng cho cả customer_app và studio_web vì chúng dùng chung package.
---

# SDUI Block

Mức 2: storefront là **danh sách block phẳng** + theme token. Không lồng nhau, không Row/Column trong JSON, không biểu thức logic trong JSON.

## Layout tách khỏi data

```
Layout config   →  đổi hiếm       →  cache lâu   →  ~5KB
Data (sản phẩm) →  đổi liên tục   →  cache ngắn  →  ~50–200KB
```

Block **khai báo** data qua `data_ref`, không **chứa** data. Server resolve sẵn mọi `data_ref` và trả cùng một response — một round-trip, không phải N.

## Hai chốt chặn trong registry — không được bỏ

```dart
final builder = _builders[node.type];

// (1) forward compat — thiếu là không bao giờ thêm được block mới
if (builder == null) {
  Telemetry.unknownBlock(node.type, node.v);
  return const SizedBox.shrink();
}

// (2) error boundary — thiếu là một shop cấu hình sai làm trắng màn hình
try {
  return builder(ctx, node, scope);
} catch (e, st) {
  Telemetry.blockRenderError(node.id, node.type, e, st);
  return const SizedBox.shrink();
}
```

## Luật parse: mọi field optional, có default

```dart
final ratio = j.dbl('aspect_ratio') ?? 16 / 9;   // ✅
final ratio = json['aspect_ratio'] as double;    // ☠️
```

Lý do cụ thể: thêm field mới ⇒ config của N shop đã lưu trong DB không có field đó ⇒ parser nghiêm khắc làm N shop trắng màn hình cùng lúc.

## Chuỗi phân giải style

```
giá trị = blockOverride ?? variantPreset ?? tenantTheme ?? appDefault
```
Áp cho **mọi** thuộc tính, qua `StyleResolver`. Không widget nào tự quyết thứ tự riêng.

## Variant là preset, không phải class
Một `ProductCard` + `ProductCardStyle.presets['elevated']`. Không tạo `ElevatedProductCard`. 4 variant × 8 block = 32 widget nếu làm sai.

## Ba tầng fallback
```
1. Server layout (fresh)      ─ lỗi ─┐
2. Cached layout (disk, ETag)  ←─────┤
3. assets/default_storefront.json ←──┘
```
Cộng kill switch `sdui_enabled` từ Remote Config.

## Thêm block mới — thứ tự
1. `contracts/blocks.registry.json` (props có default + `overridable` whitelist)
2. `make generate`
3. Widget trong `packages/ds_blocks/lib/blocks/` — pure, chỉ đọc token
4. Đăng ký trong `ds_sdui`
5. Fixture + golden test
6. Kiểm tra `studio_web`
7. `make verify-contracts && make test`

⚠️ Cần người duyệt trước. Ở Stage 0, 8 block sẵn có là đủ.

## Ba test chặn 80% sự cố
- Golden cho mọi fixture
- Layout chứa block type lạ → `returnsNormally`
- Mọi block đã đăng ký đều có ít nhất một fixture

## Không làm
Biểu thức trong JSON, block lồng block, style tự do ngoài token, `dart:io` trong `ds_blocks`.
