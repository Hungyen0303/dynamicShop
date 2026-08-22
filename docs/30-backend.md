# 30 — Backend (Spring Boot)

Đọc kèm `00-context.md` và **`70-stages.md`**. Task đụng schema/query → đọc thêm `31-database.md`.

**Stage 0** — backend là ưu tiên cao nhất. Chạy local, Postgres qua Docker, không kết nối dịch vụ ngoài nào.

---

## Bất biến — vi phạm là chặn merge

1. **`tenant_id` không bao giờ từ `@RequestParam` hoặc request body.** Chỉ từ JWT claim (authenticated plane) hoặc slug trong path (public plane).
2. **`SET LOCAL app.tenant_id`**, không bao giờ `SET`. Xem `31-database.md` — đây là lỗ rò rỉ tenant nghiêm trọng nhất.
3. **Tiền là `long`, đơn vị đồng.** Không `double`, không `BigDecimal` trong domain.
4. **`order_items` snapshot `nameSnapshot` + `unitPrice`.** Không join `products` khi trả đơn cũ.
5. **`paymentStatus` tách rời `orderStatus`.**
6. **Mọi chuyển trạng thái ghi `order_events` trong cùng transaction.**
7. **FCM chỉ gửi từ outbox worker.** Không `firebaseMessaging.send()` trong service nghiệp vụ.
8. **API public trả DTO riêng**, không serialize entity.
9. **Mọi POST tạo thực thể hoặc đụng tiền đều nhận `Idempotency-Key`.**
10. **Backend không bao giờ giữ tiền.** Không có luồng nào chuyển tiền vào tài khoản DynamicShop.

---

## Hình dạng

Một **monolith** Spring Boot 3.x, Java 21, Postgres 16. Module hoá bằng package:

```
backend/src/main/java/vn/dynamicshop/
  common/        tenant context, security, error, idempotency
  storefront/    public plane — đọc storefront, resolve data_ref
  catalog/       product, category
  order/         state machine, order_events
  payment/       payment_events, đối soát
  merchant/      API cho merchant app
  admin/         API cho ops (DataSource BYPASSRLS)
  outbox/        outbox table + worker
  notification/  FCM adapter
```

Package **không** gọi chéo repository của nhau. Giao tiếp qua service công khai của package đó.

---

## Auth ở Stage 0 — giữ đơn giản nhất có thể

JWT tự cấp, đăng nhập bằng số điện thoại + mật khẩu nằm trong fixture. **Không OTP, không Zalo, không provider ngoài.**

Nhưng **hai mặt phẳng vẫn phải đúng ngay từ đầu** — đó là kiến trúc, không phải tính năng. Sửa sau rất đắt.

---

## Hai mặt phẳng

```
/v1/s/{slug}/…        public  — tenant từ slug, không auth, chỉ đọc public + tạo đơn
/v1/merchant/…        auth    — tenant từ JWT claim
/v1/admin/…           auth    — operator, DataSource BYPASSRLS, luôn audit log
```

`TenantContext` được resolve ở filter, đặt vào `ThreadLocal` + MDC, và `SET LOCAL` được phát ở đầu mỗi transaction.

**Customer không thuộc tenant nào.** Đơn hàng thuộc `(tenant_id, user_id)`. Đừng thiết kế như thể customer đăng nhập vào một tenant.

---

## State machine đơn hàng

Nguồn sự thật: `contracts/order-states.json`. Enum và bảng transition **sinh ra** từ đó.

```
orderStatus:   PENDING → CONFIRMED → PREPARING → READY → DELIVERING → COMPLETED
                  ↓          ↓            ↓         ↓          ↓
              CANCELLED  CANCELLED    CANCELLED  CANCELLED   FAILED

paymentStatus: UNPAID | PAID | PARTIAL | REFUNDED
```

Hai trục **độc lập**. Lý do thực tế: khách chuyển khoản trước rồi quán mới xác nhận; khách trả tiền mặt lúc nhận (PAID ở bước DELIVERING); khách huỷ sau khi đã trả (CANCELLED + PAID → REFUNDED).

**Không gộp thành một enum.** Nếu thấy mình sắp thêm giá trị kiểu `CANCELLED_BUT_PAID`, đó là dấu hiệu đang vi phạm bất biến 5.

Chuyển trạng thái chỉ qua một chỗ:

```java
orderStateMachine.transition(order, to, actor, reason);
// tự kiểm tra ALLOWED, tự ghi order_events, tự ghi outbox nếu cần
```

Không `order.setStatus(...)` ở bất kỳ đâu khác.

---

## Idempotency

```
POST /v1/s/{slug}/orders
Idempotency-Key: 8f14e45f-…
```

| Tình huống | Kết quả |
|---|---|
| Trùng key + trùng `request_hash` | Trả lại response cũ (200/201 như lần đầu) |
| Trùng key + khác `request_hash` | `409 Conflict` |
| Không có header trên endpoint yêu cầu | `400` |

TTL 24h, có job dọn. Merchant app dùng cơ chế này khi flush offline queue — nên nó phải hoạt động đúng, không phải "nice to have".

---

## Outbox

```java
@Transactional
public Order createOrder(...) {
    var order = orderRepo.save(...);
    outbox.enqueue(NEW_ORDER, order.getTenantId(), order.getId(), payload);
    return order;  // FCM gửi sau, ngoài transaction
}
```

**Vì sao bắt buộc:** gọi FCM trong transaction có kịch bản FCM thành công rồi transaction rollback → chuông kêu ở quán nhưng không có đơn nào trong hệ thống. Chủ quán mất niềm tin ngay lần đầu.

Worker `@Scheduled` poll `outbox where processed_at is null`, gửi, retry với backoff, giới hạn `attempts`. At-least-once → client dedupe.

**Ở Stage 0:** làm đủ bảng `outbox` + worker, nhưng worker chỉ **ghi log** thay vì gọi FCM. Nối FCM ở Stage 1. Như vậy kiến trúc đúng ngay từ đầu mà không cần Firebase.

---

## Endpoint polling cho merchant

```
GET /v1/merchant/orders/sync?since=2026-07-29T10:15:00Z
→ { orders: [...], serverTime: "…", hasMore: false }
```

- Trả **delta** theo `updated_at`, không trả toàn bộ
- Cần index `(tenant_id, updated_at)`
- ETag / `304` khi không có gì mới — phần lớn lần poll phải tốn ~200 byte

Merchant app poll mỗi 15–20s khi foreground. Endpoint này bị gọi nhiều nhất hệ thống — giữ nó rẻ.

🔴 **`serverTime` KHÔNG phải `now()`.** Nó phải là mốc lấy **trước** khi query, **trừ thêm một biên
an toàn** lớn hơn transaction dài nhất. Lý do: `updated_at` được gán lúc flush nhưng transaction
commit sau đó, nên có cửa sổ mà một đơn đã mang mốc T1 nhưng chưa nhìn thấy được. Trả về `now()`
(T2 > T1) khiến đơn đó rơi vào khe và **không bao giờ xuất hiện lại** ở lần poll sau. Đây là lỗi
thật đã lọt qua sprint 2.1 và được sửa ở 2.1b — xem `OrderSyncService.WATERMARK_SAFETY_MARGIN`.
Cái giá là client nhận lặp các đơn trong cửa sổ đó; chấp nhận được vì client bắt buộc dedupe theo
`id` rồi (push at-least-once).

⚠️ **Mọi query dẫn xuất trên repository phải có `@Transactional`.** `@Transactional` mức class của
`SimpleJpaRepository` CHỈ phủ method kế thừa (`save`/`findById`/`findAll`), không phủ method khai
báo trên interface của mình. Không transaction → `TenantAwareJpaTransactionManager.doBegin()` không
chạy → GUC `app.tenant_id` không được set → mất tầng bảo vệ RLS. Đặt trên **từng method**, không
đặt ở mức interface (mức đó sẽ biến cả `save()` thành read-only).

---

## Storefront API — nơi dễ rò rỉ nhất

```
GET /v1/s/{slug}/storefront?schema=3
→ { layout: {…}, data: { "products:cat_x": [...], "categories": [...] } }
```

- Server **resolve mọi `data_ref`** trong layout → client chỉ cần một round-trip
- Không auth, có ETag, rate limit theo IP (Bucket4j)
- **`PublicProductDto`, không phải entity `Product`**

⚠️ Nếu serialize entity, một ngày ai đó thêm field `costPrice` và **giá vốn của quán lộ ra internet**. Có test kiểm tra response JSON không chứa key trong danh sách cấm.

---

## Đối soát thanh toán

**Trạng thái hiện tại: thủ công.** Chủ quán nhìn app ngân hàng của mình, bấm nút "Đã nhận tiền" trong merchant app → `paymentStatus = PAID`.

Tự động (Casso/SePay) là giai đoạn 2. Khi làm:

- Mã tham chiếu **ngắn** (6–8 ký tự alphanumeric) — nhiều ngân hàng cắt nội dung dài
- Webhook **idempotent** theo transaction id của ngân hàng
- **Lệch tiền → KHÔNG tự động PAID.** Thiếu hay thừa đều vào hàng đợi đối soát thủ công.
- Webhook có thể đến **trước** khi đơn commit → lưu `payment_events` độc lập, match ngược

> ⚠️ Mọi thay đổi đụng luồng tiền cần người duyệt trước.

---

## Ngày kinh doanh

```sql
tenants.timezone           DEFAULT 'Asia/Ho_Chi_Minh'
tenants.business_day_start DEFAULT '04:00'
```

Quán ăn đêm đóng cửa 2h sáng — đơn lúc 1h sáng là doanh thu của **tối hôm qua**. Cắt theo nửa đêm khiến báo cáo luôn sai và chủ quán mất niềm tin vào toàn bộ số liệu.

**Mọi tổng hợp theo ngày dùng mốc này.** Có helper `BusinessDay.of(tenant, instant)` — không tự viết lại logic.

---

## Logging

```java
MDC.put("tenantId", ctx.tenantId().toString());
MDC.put("requestId", requestId);
```

Bắt buộc. Khi chủ quán gọi "app em bị lỗi", cần grep ra ngay thay vì lục qua 200 shop.

Không log: số điện thoại đầy đủ, nội dung chuyển khoản, token. Mask trước khi log.

---

## Công thức cho task hay gặp

### Thêm một endpoint
1. Sửa `contracts/openapi.yaml` **trước**
2. `make generate` → sinh interface + client Dart
3. Implement, đặt đúng package
4. Xác định mặt phẳng: public hay authenticated → đặt đúng prefix route
5. Nếu public → viết DTO riêng + test không lộ field nhạy cảm
6. Nếu POST tạo thực thể → thêm idempotency
7. Test có cả trường hợp cross-tenant

### Thêm một trạng thái đơn
> ⚠️ Cần người duyệt.
1. Sửa `contracts/order-states.json`
2. `make generate`
3. Kiểm tra merchant app xử lý được trạng thái mới (app cũ sẽ thấy trạng thái lạ — phải không crash)

### Thêm một bảng
Xem `31-database.md`. Tóm tắt: có `tenant_id`, bật RLS, thêm vào test cô lập tenant.

---

## Test bắt buộc

| Loại | Nội dung |
|---|---|
| **Tenant isolation** | Chạy trong CI, có cả `findById`. Fail = chặn merge. Dùng Testcontainers (RLS không mock được). |
| State machine | Mọi transition hợp lệ + mọi transition không hợp lệ bị từ chối |
| Idempotency | Gửi hai lần cùng key → một bản ghi |
| Outbox | Transaction rollback → không có message nào được gửi |
| Public DTO | Response JSON không chứa key trong danh sách cấm |
| Money | Không có `double` nào trong package domain (ArchUnit) |

Chi tiết ở `docs/50-qa.md`.

---

## Không làm

| Đừng | Thay bằng |
|---|---|
| Microservices | Monolith, tách package |
| Kafka / RabbitMQ | Bảng `outbox` + `@Scheduled` |
| CQRS / Event Sourcing | CRUD + `order_events` |
| Redis (lúc này) | Caffeine in-memory |
| GraphQL | REST |
| WebSocket | FCM + polling |
| `nativeQuery = true` trong repo có tenant | JPQL hoặc Criteria; nếu buộc phải, cần duyệt + test riêng |

Mỗi thành phần hạ tầng thêm vào là một thứ có thể sập lúc 7 giờ tối và phải tự sửa.
