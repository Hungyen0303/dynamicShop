# 90 — API contract (ghi tay, Stage 0/1)

Ghi tay theo `contracts/README.md` — Stage 0 chưa dựng `contracts/openapi.yaml`/generator. File này là
nguồn FE/mobile đọc để biết shape API, **không đọc trực tiếp `.java`**. Nếu code đổi mà file này không
đổi theo, đó là bug — báo người, đừng tự đoán bên nào đúng (đúng tinh thần `AGENTS.md` mục 6).

Cập nhật lần cuối: khớp với `backend/src/main/java/vn/dynamicshop` tại thời điểm viết (Stage 1, chuẩn bị
R2/Firebase — outbox worker vẫn chỉ log/fallback, chưa có Firebase project thật).

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
| 401 | *(không có body chuẩn, `response.sendError`)* | JWT sai chữ ký/hết hạn trên route cần auth |
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

### Chưa có ở Stage 0/1 (đã nêu trong `docs/30-backend.md` nhưng chưa implement)

- `GET /v1/merchant/orders/sync?since=...` — endpoint polling cho merchant app đồng bộ đơn. **Chưa có
  controller/route nào trong code hiện tại.** Sẽ làm khi đến task merchant_app (Stage 2) hoặc khi có yêu
  cầu cụ thể — đừng gọi endpoint này, nó 404 (route không tồn tại) chứ không phải lỗi nghiệp vụ.
- Endpoint nút "Đã nhận tiền" (đổi `paymentStatus`) — chưa có route riêng; state machine hỗ trợ transition
  `paymentStatus` ở tầng `OrderStateMachine`/`Order` nhưng chưa có controller HTTP nào expose việc đổi
  `paymentStatus` độc lập với `orderStatus`. Ghi chú lại để không FE/mobile không tự đoán có endpoint này.
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

---

## 4. Ghi chú vận hành

- Tất cả tiền (`price`, `subtotal`, `shippingFee`, `discount`, `total`, `unitPrice`, `lineTotal`) là số
  nguyên `long`, đơn vị đồng — không có phần thập phân, FE không nhân/chia 1000.
- `id` mọi nơi là UUID dạng string chuẩn (`xxxxxxxx-xxxx-...`), trừ `merchantId` trong response login vốn
  cũng là UUID nhưng field khai báo kiểu `String` (giữ nguyên như code hiện tại, không phải lỗi đánh máy).
- `createdAt` là ISO-8601 UTC (`Instant` serialize mặc định của Jackson).
