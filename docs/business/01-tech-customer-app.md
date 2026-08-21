# 01 — Mobile: Customer App (Dynamic App)

*Tech spec — DynamicShop, 07/2026*

---

## Vai trò

App tổng duy nhất trên store, chứa storefront của mọi shop. Khách cài **một lần**, dùng cho mọi quán.

**Ba lý do tồn tại:**
1. Khách ở tỉnh không cài app riêng cho một quán → một app dùng chung mới có tỉ lệ cài đặt
2. Một app trên store → né được Apple Guideline 4.3 (reject app template trùng lặp)
3. Không cần store review khi thêm shop → lời hứa "lên app trong 3h" là thật

**Ràng buộc chiến lược:** đây là **hạ tầng, không phải sàn**. Không trang chủ xếp hạng, không "quán nổi bật", không bán vị trí hiển thị. Khách vào qua deep link của shop, không qua danh sách.

---

## Trục cấu hình: flavor vs runtime

Đây là quyết định kiến trúc quan trọng nhất của app này.

| Trục | Cơ chế | Số lượng |
|---|---|---|
| Môi trường | **Flavor** (build-time) | 3: `dev` / `staging` / `prod` |
| Shop / tenant | **Runtime config** (fetch từ API) | N — không giới hạn |
| App riêng (gói 3–5tr) | Flavor thứ 4 `whitelabel` + inject `DEFAULT_TENANT_ID` | Hiếm, có trả tiền |

**Không bao giờ tạo flavor cho từng shop.** 100 shop = 100 build = 100 lần submit store, và USP "3h" chết ngay.

Flavor `whitelabel` chỉ khác ở: khoá cứng một tenant, đổi app icon/tên/bundle id, ẩn phần chuyển shop. Publish dưới **developer account của chính shop**, không phải account của DynamicShop.

---

## Server-Driven UI — mức 2

### Phạm vi

| Dùng SDUI | Hardcode hoàn toàn |
|---|---|
| Home, danh mục, danh sách/chi tiết sản phẩm, trang khuyến mãi | **Giỏ hàng, checkout, thanh toán, đăng nhập, theo dõi đơn** |

Lý do hardcode nhóm phải: logic phức tạp, rủi ro tiền bạc, và chủ shop không có nhu cầu custom. Ranh giới này giữ cho bạn sửa được bug thanh toán trong 20 phút thay vì điều tra shop nào cấu hình sai.

### Tách Layout khỏi Data

```
Layout config   →  đổi hiếm       →  cache lâu   →  ~5KB
Data (sản phẩm) →  đổi liên tục   →  cache ngắn  →  ~50–200KB
```

Block **không chứa** data, block **khai báo** data nó cần:

```json
{
  "schema_version": 3,
  "theme": {
    "primary": "#E23744", "surface": "#FFFFFF",
    "radius_md": 12, "spacing": 8,
    "font_family": "Be Vietnam Pro"
  },
  "screens": {
    "home": {
      "blocks": [
        { "id": "hero_1", "type": "hero_banner", "v": 1,
          "props": { "image_url": "...", "aspect_ratio": 2.0 } },
        { "id": "grid_1", "type": "product_grid", "v": 2,
          "props": { "columns": 2, "card_style": "elevated" },
          "data_ref": "products:cat_do_uong" }
      ]
    }
  }
}
```

### Một round-trip, không phải N

Sai lầm kinh điển: mỗi block tự gọi API. 8 block = 8 request = 4–5 giây màn hình trắng trên 3G.

```
GET /v1/s/{slug}/storefront?schema=3

{
  "layout": { ... },
  "data": {
    "products:cat_do_uong": [ ... ],
    "categories": [ ... ]
  }
}
```

Server resolve sẵn mọi `data_ref`. Đây là khác biệt giữa 800ms và 5 giây — trên thị trường này, đó là khác biệt giữa "app mượt" và "app lag".

### Bộ block khởi đầu — giữ đúng 8 cái trong 6 tháng đầu

| Type | Props | Data ref |
|---|---|---|
| `hero_banner` | `image_url`, `aspect_ratio`, `title?`, `action?` | — |
| `category_row` | `style` (chip\|circle), `show_all` | `categories` |
| `product_grid` | `columns` (2\|3), `card_style`, `show_price` | `products:{cat}` |
| `product_list` | `show_thumbnail`, `dense` | `products:{cat}` |
| `promo_strip` | `text`, `bg_color_token`, `dismissible` | — |
| `info_card` | `title`, `body`, `icon` | — |
| `image_banner` | `image_url`, `action` | — |
| `spacer` | `height_units` | — |

Thêm block mới chỉ khi có **≥3 shop cùng yêu cầu**.

`action` là union hẹp, không phải URL tự do — để kiểm soát deep link, analytics, và không cho shop nhét link lạ:

```json
{ "action": { "type": "open_category", "id": "cat_1" } }
{ "action": { "type": "open_product",  "id": "p_9" } }
{ "action": { "type": "external_url",  "url": "https://..." } }
```

### Chuỗi phân giải style — viết một lần, dùng mọi nơi

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

Bốn tầng, một quy tắc, áp dụng cho **mọi** thuộc tính. Không để mỗi widget tự quyết thứ tự — đó là nguồn gốc của bug "shop đổi theme mà nút không đổi màu".

### Whitelist thuộc tính được override

```dart
const kOverridable = <String, Set<String>>{
  'product_grid': {'radius', 'bg', 'card_style', 'columns'},
  'hero_banner':  {'radius', 'aspect_ratio'},
  'promo_strip':  {'bg'},              // không cho đổi fg — xem dưới
  'category_row': {'style'},
  'spacer':       {'height'},
};
```

Ba lý do phải khoá: refactor block sau này không làm hỏng shop đang chạy; studio tự sinh form từ bảng này; và không có whitelist thì tổ hợp cần test là vô hạn.

### Tự suy màu chữ — một dòng chặn cả một loại bug

Chủ quán sẽ chọn `primary` = vàng nhạt và chữ trắng biến mất.

```dart
Color onColorOf(Color bg) =>
    bg.computeLuminance() > 0.5 ? const Color(0xFF1A1A1A) : Colors.white;
```

Khách chỉ chọn màu nền, màu chữ tự tính.

### Ba tầng fallback — không bao giờ màn hình trắng

```
1. Server layout (fresh)        ─ fail/parse error ─┐
2. Cached layout (disk, ETag)   ←──────────────────┤
3. Bundled default (assets/)    ←──────────────────┘
```

Tầng 3 quan trọng hơn nó trông: ship `assets/default_storefront.json`. Server chết, schema lỗi, shop chưa cấu hình — app vẫn hiển thị storefront tử tế.

Kết hợp **Firebase Remote Config làm kill switch**: push nhầm schema lỗi cho 40 shop lúc 7h tối → bật `sdui_enabled = false`, tất cả rơi về bundled trong 30 giây, thay vì chờ 3 ngày review store. Đây là lý do duy nhất Remote Config đáng có trong stack.

### Registry — hai chốt chặn giữ hệ thống sống

```dart
Widget build(BuildContext ctx, BlockNode node, BlockScope scope) {
  final builder = _builders[node.type];

  // (1) Forward compat — block lạ thì bỏ qua im lặng
  if (builder == null) {
    Telemetry.unknownBlock(node.type, node.v);
    return const SizedBox.shrink();
  }

  // (2) Error boundary — một block hỏng không giết cả màn hình
  try {
    return builder(ctx, node, scope);
  } catch (e, st) {
    Telemetry.blockRenderError(node.id, node.type, e, st);
    return const SizedBox.shrink();
  }
}
```

Không có `(1)` → không bao giờ thêm được block mới. Không có `(2)` → một shop nhập sai một field là storefront trắng bóc lúc 7h tối.

`Telemetry.unknownBlock` không phải cho đẹp: nó cho biết bao nhiêu % người dùng đang chạy app cũ, tức là biết khi nào an toàn để bỏ hỗ trợ block cũ.

### Luật parse: mọi field optional, có default

```dart
// ☠️ throw khi field thiếu / null / int thay vì double
final ratio = json['aspect_ratio'] as double;

// ✅
final ratio = j.dbl('aspect_ratio') ?? 16 / 9;

extension Json on Map<String, dynamic> {
  String? str(String k) => this[k] is String ? this[k] as String : null;
  double? dbl(String k) => (this[k] as num?)?.toDouble();  // nhận cả int
  bool bl(String k, {bool or = false}) => this[k] is bool ? this[k] as bool : or;
}
```

**Đây là toàn bộ "forward compatibility" cần thiết.** Lý do rất cụ thể: 4 tháng nữa thêm `radius_lg` vào tokens; config của 80 shop đã lưu trong DB không có field đó. Parser nghiêm khắc = 80 shop trắng màn hình cùng lúc.

---

## Công nghệ tích hợp

| Công nghệ | Why | How |
|---|---|---|
| **Flutter + melos** | Monorepo, share package giữa 2 app + studio web | `melos bootstrap`, packages nội bộ dùng `path:` |
| **FCM** | Push trạng thái đơn cho khách | Notification message (khách không cần data message) |
| **Crashlytics** | Bắt crash trên máy thật, máy tỉnh cấu hình lạ | Bắt buộc từ ngày đầu |
| **Remote Config** | **Kill switch SDUI** + feature flag | Fetch lúc khởi động, cache 1h |
| **Firebase Analytics** | Funnel: xem → thêm giỏ → đặt | Event chuẩn hoá, gắn `tenant_id` làm user property |
| **Zalo Login** | ~100% khách VN có Zalo; **rẻ hơn SMS OTP rất nhiều** và chuyển đổi cao hơn | Native SDK qua platform channel → gửi token lên Spring → Spring cấp JWT riêng |
| **SMS OTP** | Dự phòng cho khách không dùng Zalo | Nhà cung cấp nội địa (eSMS…), không dùng Firebase Phone Auth (đắt) |
| **App Links / Universal Links** | Khách bấm link trong Zalo/FB → mở **thẳng storefront**, không qua danh sách | `assetlinks.json` + `apple-app-site-association`; fallback về web nếu chưa cài app |
| **dio + retry interceptor** | Mạng 3G tỉnh chập chờn | Exponential backoff, ETag, timeout 10s |
| **cached_network_image** | Ảnh menu chiếm phần lớn băng thông | Cache disk; **URL phải trỏ CDN đã resize**, không phải ảnh gốc |
| **google_fonts + font bundled** | Font phải đủ dấu tiếng Việt | Bundle 2–3 font phổ biến nhất; còn lại tải runtime + cache |
| **Riverpod** (gợi ý) | State + DI, test được, không cần context | Chọn một và giữ nguyên; điều tệ nhất là trộn nhiều giải pháp |
| **Hive / file cache** | Lưu layout + tokens giữa các lần mở app | Key theo `(tenant_id, schema_version)` |
| **Sentry** | Lỗi non-fatal + breadcrumb + performance | Bổ sung cho Crashlytics, không thay thế |

### Cái bẫy font tiếng Việt

Rất nhiều font đẹp **không có đủ dấu tiếng Việt** — chữ hiện ra kiểu "Trà s?a" hoặc dấu lệch, và bạn chỉ phát hiện khi shop gửi ảnh chụp màn hình.

- **Không cho gõ tên font tự do.** Dropdown danh sách đã kiểm chứng.
- An toàn: *Be Vietnam Pro, Inter, Roboto, Nunito Sans, Lexend, Open Sans, Montserrat, Quicksand*
- **Preload font của tenant trước khi render frame đầu**, nếu không sẽ có nháy đổi font rất xấu

---

## Cấu trúc package

```
packages/
  ds_tokens/       ThemeExtension, design token, không phụ thuộc gì
  ds_components/   DsButton, DsInputBar, DsCard, DsChip...
  ds_blocks/       widget block, pure, không network
  ds_sdui/         registry + renderer json→widget
  ds_api/          client, DTO, model
  ds_core/         routing, storage, utils
apps/
  customer_app/
```

`ds_blocks` + `ds_sdui` + `ds_tokens` được dùng chung bởi app **và** studio web — đó là lý do preview trong studio không bao giờ lệch so với app thật.

### Chốt chặn CI: lint chống hardcode

Không có bước này, sau 3 tháng ~30% widget sẽ lén hardcode màu và multi-style hỏng **im lặng** — chỉ vài shop bị, bạn không biết cho tới khi họ phàn nàn.

```bash
if grep -rnE "Colors\.|Color\(0x|BorderRadius\.circular\([0-9]" \
     packages/ds_blocks/lib packages/ds_components/lib; then
  echo "❌ Hardcoded style trong package dùng chung"; exit 1
fi
```

---

## Test

```dart
// 1. Golden test cho mọi fixture layout
for (final f in loadFixtures('test/fixtures/layouts')) {
  testGoldens('storefront: ${f.name}', (t) async {
    await t.pumpWidget(StorefrontRenderer(layout: f.layout, data: f.data));
    await screenMatchesGolden(t, f.name);
  });
}

// 2. Block lạ KHÔNG được crash
test('unknown block renders gracefully', () {
  expect(() => registry.buildAll(layoutWithUnknownBlock), returnsNormally);
});

// 3. Mọi block đã đăng ký đều phải có fixture
test('every registered block has a golden fixture', () {
  expect(registry.registeredTypes.difference(coveredTypes), isEmpty);
});
```

Bài 2 và 3 chặn khoảng 80% sự cố SDUI trên production. Bài 3 ép mỗi block mới phải có ảnh golden → phát hiện regression giao diện trong CI thay vì qua tin nhắn của chủ quán.

---

## Mạng yếu — thiết kế cho 3G tỉnh

- Mở app → render **cache ngay lập tức**, fetch nền, chỉ update nếu ETag đổi
- Ảnh: CDN resize, `cached_network_image`, placeholder blur
- Timeout 10s, retry backoff, **không bao giờ spinner toàn màn hình** khi đã có cache
- Giỏ hàng lưu local, không mất khi mất mạng
- Đặt hàng gửi kèm `Idempotency-Key` (UUID client sinh, giữ nguyên khi retry)

---

## Checklist trước khi ship

- [ ] Deep link từ Zalo/FB mở thẳng storefront, có fallback web
- [ ] Bundled default layout tồn tại và đã test bằng cách chặn API
- [ ] Kill switch Remote Config đã thử trên máy thật
- [ ] Golden test cả 8 block, test unknown-block pass
- [ ] Lint hardcode chạy trong CI
- [ ] Font tenant preload trước frame đầu, không nháy
- [ ] Idempotency-Key gửi kèm khi tạo đơn
- [ ] Test trên máy Android tầm thấp (2GB RAM) với mạng bị bóp
