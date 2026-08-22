# 90 — API contract (ghi tay, Stage 0/1)

Ghi tay theo `contracts/README.md` — Stage 0 chưa dựng `contracts/openapi.yaml`/generator. File này là
nguồn FE/mobile đọc để biết shape API, **không đọc trực tiếp `.java`**. Nếu code đổi mà file này không
đổi theo, đó là bug — báo người, đừng tự đoán bên nào đúng (đúng tinh thần `AGENTS.md` mục 6).

Cập nhật lần cuối: **sprint 2.1b** (2026-08-22) — thêm `GET /v1/merchant/orders/{id}` (chi tiết đơn kèm
dòng món), đổi `server_time`/`has_more` sang camelCase, sửa mốc `serverTime` (lỗi mất đơn im lặng), và
mọi route thiếu/sai xác thực nay trả **401** thay vì 403.

Trước đó — sprint 2.1 (2026-08-22) — thêm 3 endpoint merchant: đồng bộ đơn (polling), nút "Đã
nhận tiền", đăng ký/thu hồi device token FCM. Outbox worker giờ đã fan-out tới device token thật của đúng
tenant, nhưng `FcmSender` vẫn là `LogOnlyFcmSender` cho tới khi có Firebase project thật
(`missing_config.md` mục 3).

---

## Hai mặt phẳng

| Mặt phẳng | Prefix | Tenant lấy từ | Auth |
|---|---|---|---|
| Public | `/v1/s/{slug}/...` | slug trong path (filter resolve, set vào `TenantContext`) | Không |
| Merchant auth (bootstrap) | `POST /v1/merchant/{slug}/auth/login` | slug trong path (chỉ route này) | Không (đây là bước lấy JWT) |
| Merchant (đã đăng nhập) | `/v1/merchant/...` (trừ `/auth/login`) | JWT claim `tenant_id` | Bắt buộc — header `Authorization: Bearer <jwt>` |
| Admin | `/v1/admin/...` | JWT claim | Bắt buộc — **chưa có controller nào ở Stage 0/1** |

`slug` sai (không tồn tại tenant) → `404 TENANT_NOT_FOUND` trước khi vào tới controller, cho cả public
plane lẫn route login.

---

## Error shape chung

Mọi lỗi (trừ lỗi hạ tầng ngoài tầm `GlobalExceptionHandler`, ví dụ 401 do JWT sai chữ ký ném thẳng từ
filter) trả về:

```json
{
  "code": "SOME_ERROR_CODE",
  "message": "Mô tả cho người, tiếng Việt"
}
```

| HTTP status | code | Khi nào |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Bean Validation fail (`@NotBlank`, `@NotNull`, `@Min`, `@NotEmpty`...) |
| 400 | `ILLEGAL_STATE` | `IllegalStateException` bất kỳ (ví dụ lỗi serialize nội bộ) |
| 400 | `MISSING_IDEMPOTENCY_KEY` | Thiếu header `Idempotency-Key` trên endpoint yêu cầu |
| 400 | `PRODUCT_NOT_FOUND` | `productId` trong đơn không tồn tại/đã xoá |
| 400 | `INVALID_STATUS_VALUE` | `to` trong transition không khớp enum `OrderStatus` |
| 400 | `INVALID_PAYMENT_STATUS_VALUE` | `to` trong `/payment` không khớp enum `PaymentStatus` |
| 400 | `INVALID_SINCE` | `since` trong `/sync` không phải ISO-8601 UTC hợp lệ |
| 400 | `INVALID_PLATFORM` | `platform` khi đăng ký device không phải `ANDROID`/`IOS` |
| 401 | `UNAUTHENTICATED` | **Thiếu** header `Authorization`, HOẶC JWT sai chữ ký/hết hạn, trên bất kỳ route cần auth |
| 401 | `INVALID_CREDENTIALS` | Sai số điện thoại/mật khẩu lúc login merchant |
| 404 | `TENANT_NOT_FOUND` | Slug không khớp tenant nào |
| 404 | `ORDER_NOT_FOUND` | `orderId` không tồn tại |
| 404 | `STOREFRONT_NOT_CONFIGURED` | Tenant chưa có row `storefronts` |
| 409 | `IDEMPOTENCY_KEY_CONFLICT` | Cùng `Idempotency-Key`, khác nội dung request |
| 409 | `INVALID_ORDER_TRANSITION` | Chuyển `orderStatus` không hợp lệ theo `contracts/order-states.json` |
| 409 | `INVALID_PAYMENT_TRANSITION` | Chuyển `paymentStatus` không hợp lệ |

---

## 1. Public plane

### `GET /v1/s/{slug}/storefront?schema=3`

Không auth, không cần header đặc biệt.

Query param: `schema` (int, optional, mặc định `3`) — **hiện KHÔNG được backend dùng để đổi hành vi**, để
sẵn chỗ cho versioning sau, luôn trả schema hiện tại bất kể giá trị truyền vào.

Response `200`:

```json
{
  "layout": {
    "schema_version": 3,
    "theme": { "...": "theo contracts/tokens.json, tuỳ tenant" },
    "screens": { "...": "cấu trúc theo contracts/storefront.schema.json" }
  },
  "data": {
    "categories": [
      { "id": "uuid", "name": "string", "sortOrder": 0 }
    ],
    "products:{categoryId}": [
      { "id": "uuid", "name": "string", "price": 45000, "imageUrl": "string|null", "available": true }
    ]
  }
}
```

Ghi chú:
- `data` chỉ chứa các key mà `layout.screens[*].blocks[*].data_ref` yêu cầu (server tự duyệt, không phải
  toàn bộ catalog). `data_ref` lạ không khớp `categories`/`products:{uuid}` bị bỏ qua âm thầm, không lỗi.
- `PublicProductDto`/`PublicCategoryDto` — DTO riêng, không có `tenantId`, `costPrice`, hay field nội bộ
  nào khác (bất biến #8, có test kiểm tra).
- `price` là `long`, đơn vị đồng.
- 404 `STOREFRONT_NOT_CONFIGURED` nếu tenant chưa có storefront.

---

### `POST /v1/s/{slug}/orders`

Header bắt buộc: `Idempotency-Key: <uuid hoặc string bất kỳ>` — thiếu → `400 MISSING_IDEMPOTENCY_KEY`.

Request body:

```json
{
  "items": [
    { "productId": "uuid", "qty": 2, "options": { "any": "json, optional" } }
  ],
  "note": "string|null",
  "deliveryAddress": "string|null",
  "phone": "string|null"
}
```

Validation: `items` không được rỗng; mỗi item `productId` bắt buộc, `qty >= 1`.

Response `201 Created` — `OrderResponseDto`:

```json
{
  "id": "uuid",
  "code": "OD1A2B3C4D",
  "orderStatus": "PENDING",
  "paymentStatus": "UNPAID",
  "subtotal": 90000,
  "shippingFee": 0,
  "discount": 0,
  "total": 90000,
  "note": "string|null",
  "deliveryAddress": "string|null",
  "phone": "string|null",
  "items": [
    { "nameSnapshot": "string", "unitPrice": 45000, "qty": 2, "lineTotal": 90000 }
  ],
  "createdAt": "2026-08-22T10:00:00Z"
}
```

Idempotency (bất biến #9, xem `docs/30-backend.md`):
- Cùng `Idempotency-Key` + cùng nội dung request → trả lại **response cũ nguyên vẹn** (status + body),
  không tạo đơn thứ hai.
- Cùng key, nội dung khác → `409 IDEMPOTENCY_KEY_CONFLICT`.
- Item không tồn tại/đã xoá → `400 PRODUCT_NOT_FOUND`.
- `shippingFee`/`discount` hiện luôn `0` (chưa có logic tính phí ship/giảm giá ở Stage 0/1).
- Đơn tạo mới luôn `orderStatus = PENDING`, `paymentStatus = UNPAID`.

---

## 2. Merchant plane

### `POST /v1/merchant/{slug}/auth/login`

Không cần `Authorization` header (đây là bước lấy token). Không cần `Idempotency-Key` (không tạo thực
thể, không đụng tiền — chỉ phát token).

Request:

```json
{ "phone": "string", "password": "string" }
```

Response `200`:

```json
{
  "token": "eyJ...",
  "merchantId": "uuid-as-string",
  "name": "string",
  "role": "OWNER|STAFF"
}
```

Sai thông tin đăng nhập → `401 INVALID_CREDENTIALS`.

JWT claims bên trong token (để FE/mobile hiểu, không phải để tự parse thay vì gọi API):
`sub` = merchantId, `tenant_id` = uuid tenant, `role` = tên role.

Dùng token này trong header `Authorization: Bearer <token>` cho mọi route `/v1/merchant/...` khác.

---

### `POST /v1/merchant/orders/{id}/transition`

Auth bắt buộc (`Authorization: Bearer <jwt>`). Tenant lấy từ JWT claim — **không** truyền tenant qua path
này (path chỉ có `{id}` là orderId).

**Không yêu cầu `Idempotency-Key`** hiện tại — đây là chuyển trạng thái trên một order đã tồn tại
(`orderId` cố định), không tạo thực thể mới, nên state machine tự nhiên idempotent theo nghĩa "gọi lại
cùng target status trên order đã ở status khác sẽ bị `409 INVALID_ORDER_TRANSITION`" — không có rủi ro
tạo đúp giống `POST /orders`.

Request:

```json
{ "to": "CONFIRMED", "reason": "string|null" }
```

`to` phải là một giá trị hợp lệ trong `OrderStatus` (`PENDING|CONFIRMED|PREPARING|READY|DELIVERING|COMPLETED|CANCELLED|FAILED`).
Sai giá trị (không khớp enum) → `400 INVALID_STATUS_VALUE`. Đúng enum nhưng không phải transition hợp lệ
theo `contracts/order-states.json` → `409 INVALID_ORDER_TRANSITION`.

Response `200` — `OrderResponseDto` (shape giống hệt response tạo đơn ở trên, với `orderStatus` đã cập
nhật).

Order không tồn tại → `404 ORDER_NOT_FOUND`.

Mỗi lần transition thành công ghi một dòng `order_events` trong cùng transaction (bất biến #6) và enqueue
một dòng outbox `ORDER_STATUS_CHANGED` (worker xử lý sau, ngoài transaction).

---

### `GET /v1/merchant/orders/{id}`

Auth bắt buộc. Trả `OrderResponseDto` **đầy đủ** — shape giống hệt response tạo đơn, **có `items[]`**.

Đây là đường lấy dòng món; `/sync` cố ý không trả `items` để giữ endpoint poll rẻ. Màn danh sách của
merchant_app dùng `/sync`, màn chi tiết gọi route này khi chủ quán mở một đơn.

Order không tồn tại **hoặc thuộc tenant khác** → `404 ORDER_NOT_FOUND` (không phải 403 — không xác
nhận cho người gọi biết orderId đó có tồn tại ở đâu đó hay không).

---

### `GET /v1/merchant/orders/sync`

Auth bắt buộc. Đây là **kênh nhận đơn foreground** của merchant_app (bất biến #1,
`docs/11-merchant-app.md`: hai kênh — FCM khi nền, polling 15–20s khi mở app). Endpoint bị gọi nhiều
nhất hệ thống, mọi thứ ở đây đều tối ưu cho việc "rẻ".

Query param:

| Param | Kiểu | Bắt buộc | Ý nghĩa |
|---|---|---|---|
| `since` | ISO-8601 UTC (`2026-08-22T10:15:00Z`) | Không | Chỉ trả đơn có `updated_at >= since`. Bỏ trống = đồng bộ đầy đủ từ đầu (lần cài app đầu). |

Header:

| Header | Bắt buộc | Ý nghĩa |
|---|---|---|
| `Authorization: Bearer <jwt>` | **Có** | |
| `If-None-Match: <etag lần trước>` | Không, **nhưng nên luôn gửi** | Trùng ETag → `304`, không có body |

Response `200`:

```json
{
  "orders": [
    {
      "id": "uuid",
      "code": "OD1A2B3C4D",
      "orderStatus": "PENDING",
      "paymentStatus": "UNPAID",
      "total": 90000,
      "phone": "string|null",
      "deliveryAddress": "string|null",
      "note": "string|null",
      "itemCount": 3,
      "createdAt": "2026-08-22T10:00:00Z",
      "updatedAt": "2026-08-22T10:00:00Z"
    }
  ],
  "serverTime": "2026-08-22T10:10:00Z",
  "hasMore": false
}
```

🔴 **`serverTime` KHÔNG phải "bây giờ" — nó là mốc đã lùi lại 60 giây, cố ý.** Đừng "sửa" bằng cách
lấy đồng hồ máy cho gần thực tế hơn. Lý do: `updated_at` của đơn được gán lúc ghi nhưng transaction
commit sau đó vài mili giây; nếu mốc trả về mới hơn khoảng đó, đơn nằm trong khe sẽ **không bao giờ
xuất hiện lại**. Biên 60 giây che khe đó. Hệ quả anh sẽ thấy khi test tay: **các đơn trong 60 giây gần
nhất lặp lại ở lần poll kế tiếp** — đúng như thiết kế, dedupe theo `id` là xong.

(Sprint 2.1 từng trả `server_time`/`has_more` snake_case. Đã đổi sang camelCase ở 2.1b, lúc chưa có
client nào phụ thuộc.)

Ràng buộc mà client **phải** tuân theo — đọc kỹ trước khi viết merchant_app:

1. **Không có `items`.** Bản tóm tắt cố ý bỏ dòng món để tiết kiệm băng thông; `itemCount` đủ cho màn
   danh sách. Muốn chi tiết món thì mở từng đơn (endpoint chi tiết chưa có — sprint sau).
2. **Trang tối đa 50 đơn, client KHÔNG đổi được** (không có `?limit=`). `has_more: true` nghĩa là gọi
   tiếp với `since` = `updatedAt` của đơn cuối cùng nhận được.
3. **Lấy mốc `since` cho lần sau từ `serverTime`, KHÔNG lấy từ đồng hồ máy và KHÔNG tự tính từ
   `updatedAt` của đơn.** Đồng hồ điện thoại lệch vài phút là chuyện thường; và `max(updatedAt)` của
   trang vừa nhận cũng không an toàn (có thể mới hơn một đơn đang commit dở). `serverTime` là giá trị
   duy nhất đã tính sẵn biên an toàn — dùng đúng nó.
4. **Bộ lọc là `>=` chứ không phải `>`** → đơn ở đúng mốc `since` sẽ xuất hiện lại ở lần gọi sau.
   Client **bắt buộc dedupe theo `id`**. (Việc này vốn đã bắt buộc vì push là at-least-once — bất biến
   #4.) Lý do dùng `>=`: nhiều đơn có thể trùng `updated_at`, dùng `>` sẽ nhảy mất phần còn lại của
   nhóm trùng mốc khi trang bị cắt ngang giữa nhóm.
5. **Luôn gửi lại `If-None-Match`.** Không gửi thì app tự kéo cả trang JSON mỗi 15 giây suốt ngày trên
   4G, dù không có gì mới.

`since` sai định dạng → `400 INVALID_SINCE`.

---

### `POST /v1/merchant/orders/{id}/payment`

Auth bắt buộc. Header `Idempotency-Key`: **BẮT BUỘC** — thiếu → `400 MISSING_IDEMPOTENCY_KEY`.

Đây là nút "Đã nhận tiền". Khác với `/transition` ngay trên (không cần key): route này đụng tiền, và
merchant_app đẩy mọi hành động qua offline queue có retry (bất biến #3) — một lần retry trên mạng chập
chờn không được phép thành hai lần ghi nhận thu tiền.

Request:

```json
{ "to": "PAID", "reason": "string|null" }
```

`to` ∈ `UNPAID | PARTIAL | PAID | REFUNDED`. Sai giá trị (không khớp enum) → `400
INVALID_PAYMENT_STATUS_VALUE`. Đúng enum nhưng không phải transition hợp lệ theo
`contracts/order-states.json#/payment_status` → `409 INVALID_PAYMENT_TRANSITION`.

Response `200` — `OrderResponseDto` (shape giống response tạo đơn), với `paymentStatus` đã cập nhật.

🔴 **`orderStatus` KHÔNG đổi theo** — hai trục độc lập (bất biến #5). Quán nhận tiền trước khi giao xong
là chuyện bình thường, và ngược lại cũng vậy. Đừng suy ra trạng thái đơn từ trạng thái thanh toán.

Idempotency ở đây tính hash trên `(orderId + body)`, nên cùng một `Idempotency-Key` dùng lại cho **đơn
khác** sẽ ra `409 IDEMPOTENCY_KEY_CONFLICT` chứ không âm thầm trả về đơn cũ.

Order không tồn tại (hoặc thuộc tenant khác) → `404 ORDER_NOT_FOUND`.

Mỗi lần đổi thành công ghi một dòng `order_events` với `to_status = "PAYMENT:<trạng thái>"` (tiền tố
`PAYMENT:` để phân biệt với trục `order_status` vì hai trục dùng chung bảng), và enqueue outbox
`ORDER_PAYMENT_CHANGED`.

---

### `POST /v1/merchant/devices` — đăng ký FCM token

Auth bắt buộc. Không cần `Idempotency-Key` (upsert theo token, idempotent tự nhiên).

merchant_app gọi route này **sau khi đăng nhập** và **mỗi lần FCM xoay token**.

Request:

```json
{ "token": "chuỗi opaque từ FCM", "platform": "ANDROID", "appVersion": "1.0.0" }
```

`platform` ∈ `ANDROID | IOS`, không phân biệt hoa thường. Sai → `400 INVALID_PLATFORM`.
`appVersion` optional.

Response `200`:

```json
{ "id": "uuid", "platform": "ANDROID", "appVersion": "1.0.0", "lastSeenAt": "2026-08-22T10:00:00Z" }
```

Cố ý **không trả lại `token`** — client vừa gửi nó lên, không có lý do để nó xuất hiện thêm lần nữa
trong log/proxy trên đường về.

Đăng ký lại cùng token → cập nhật dòng cũ, **không** tạo dòng thứ hai (nếu không outbox worker sẽ gửi
trùng lên cùng một máy).

### `DELETE /v1/merchant/devices` — thu hồi khi đăng xuất

Auth bắt buộc. Token đi trong **body**, không phải query param (query param sẽ nằm lại trong access log
của Caddy và mọi proxy trên đường đi; token đó đủ để đẩy thông báo giả tới máy chủ quán).

```json
{ "token": "chuỗi opaque từ FCM" }
```

Response `204 No Content`. Token không tồn tại cũng trả `204` — đăng xuất phải luôn thành công phía
người dùng.

⚠️ **Push hiện vẫn chưa gửi thật.** Đường ống đã đủ (đơn mới → outbox → worker → resolve token đúng
tenant → gọi sender), nhưng `FcmSender` còn là `LogOnlyFcmSender` cho tới khi có service-account Firebase
thật (`missing_config.md` mục 3). Đăng ký token vẫn có tác dụng ngay: nó là dữ liệu mà sprint 2.5 chỉ
việc cắm vào.

---

### Chưa có (ghi lại để FE/mobile không tự đoán)

- Upload ảnh sản phẩm — chưa có endpoint HTTP nào (`Product.imageUrl` hiện là String field, chưa có
  consumer thật ở Stage 0/1; hạ tầng `ImageStorageService` được dựng sẵn ở Stage 1 nhưng chưa nối HTTP
  endpoint — xem package `common/storage`).
- `/v1/admin/...` — chưa có controller nào.

---

## 3. Header tổng hợp theo endpoint

| Endpoint | `Authorization` | `Idempotency-Key` |
|---|---|---|
| `GET /v1/s/{slug}/storefront` | Không | Không |
| `POST /v1/s/{slug}/orders` | Không | **Bắt buộc** |
| `POST /v1/merchant/{slug}/auth/login` | Không | Không |
| `POST /v1/merchant/orders/{id}/transition` | **Bắt buộc** | Không |
| `GET /v1/merchant/orders/sync` | **Bắt buộc** | Không (nên gửi `If-None-Match`) |
| `GET /v1/merchant/orders/{id}` | **Bắt buộc** | Không |
| `POST /v1/merchant/orders/{id}/payment` | **Bắt buộc** | **Bắt buộc** |
| `POST /v1/merchant/devices` | **Bắt buộc** | Không |
| `DELETE /v1/merchant/devices` | **Bắt buộc** | Không |

---

## 4. Ghi chú vận hành

- Tất cả tiền (`price`, `subtotal`, `shippingFee`, `discount`, `total`, `unitPrice`, `lineTotal`) là số
  nguyên `long`, đơn vị đồng — không có phần thập phân, FE không nhân/chia 1000.
- `id` mọi nơi là UUID dạng string chuẩn (`xxxxxxxx-xxxx-...`), trừ `merchantId` trong response login vốn
  cũng là UUID nhưng field khai báo kiểu `String` (giữ nguyên như code hiện tại, không phải lỗi đánh máy).
- `createdAt`/`updatedAt`/`lastSeenAt`/`server_time` là ISO-8601 UTC (`Instant` serialize mặc định của
  Jackson).
- **JWT merchant sống 30 ngày** (`app.jwt.expiration-minutes: 43200`). Không có refresh token — quyết
  định của chủ dự án (`progress.md` mục 3, #12). merchant_app phải **lưu số điện thoại + mật khẩu an
  toàn trên máy và tự gọi lại `/auth/login` khi gặp `401`**; không làm việc này thì app sẽ im lặng ngừng
  nhận đơn khi token hết hạn giữa ca.
- 🔴 **`401` là tín hiệu DUY NHẤT để đăng nhập lại, và nó phủ cả trường hợp thiếu header.** Sprint 2.1
  từng để Spring trả `403` mặc định khi thiếu `Authorization`; đã sửa ở 2.1b vì `403` nằm ngoài luật
  re-login, nên một lỗi rơi header ở client sẽ biến thành **im lặng ngừng nhận đơn**. Nay cả hai
  trường hợp đều ra `401` với body `{"code":"UNAUTHENTICATED","message":"..."}` — cùng shape lỗi với
  phần còn lại của API.
- **Luật cho merchant_app**: gặp `401` → thử `POST /v1/merchant/{slug}/auth/login` lại MỘT lần bằng
  số điện thoại + mật khẩu đã lưu, rồi gọi lại request cũ. Thất bại lần hai mới báo người dùng. Phòng
  thủ thêm: nếu vì lý do nào đó nhận `403` trên route merchant, xử lý y như `401` — không có route
  merchant nào dùng `403` cho mục đích khác.
