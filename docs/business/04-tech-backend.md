# 04 — Backend: Spring Boot Multi-Tenant

*Tech spec — DynamicShop, 07/2026*

---

## Hình dạng tổng thể

**Một monolith Spring Boot**, tách module bằng package. Postgres. Chạy trên một VPS Singapore với Docker Compose.

Ba thứ đắt nhất để sửa sau — làm đúng ngay từ tuần đầu:

1. **Cô lập tenant**
2. **Snapshot giá trong `order_items`**
3. **Tách `payment_status` khỏi `order_status`**

Mọi thứ còn lại đều sửa được.

---

## 1. `tenant_id` đến từ đâu — luật số một

```java
// ☠️ TUYỆT ĐỐI KHÔNG
@GetMapping("/orders")
List<Order> list(@RequestParam UUID tenantId) { ... }
```

`tenant_id` **không bao giờ** lấy từ input của client.

### Hai mặt phẳng, hai mô hình bảo mật

Hệ thống này có một điểm đặc biệt mà đa số tutorial multi-tenant không nói tới:

> **Khách hàng không thuộc về tenant nào.** Một khách trong Dynamic App có thể đặt ở 10 quán khác nhau.

| | Public plane | Authenticated plane |
|---|---|---|
| Ai | Khách (Dynamic App) | Chủ shop (merchant), admin |
| Tenant từ đâu | **Slug trong route** `/v1/s/{slug}/…` | **JWT claim** |
| Được làm gì | Đọc dữ liệu public + tạo đơn cho chính mình | Toàn quyền trong tenant của mình |
| Identity | `user_id` toàn cục (Zalo) | `user_id` + `tenant_id` |

Đơn hàng thuộc `(tenant_id, user_id)`. Khách đọc storefront của tenant X mà không "ở trong" tenant X. **Thiết kế nhầm chỗ này phải viết lại toàn bộ tầng bảo mật.**

---

## 2. Cô lập tenant — hai tầng

```
Hibernate @TenantId  →  nhanh, tự động, app-level     (cứu 99%)
Postgres RLS         →  người từ chối CUỐI CÙNG        (cứu 1% còn lại)
```

Hibernate filter cứu hầu hết. RLS cứu lúc ai đó viết native query quên `WHERE`.

### Hibernate 6

```java
@Entity
public class Order {
  @Id UUID id;
  @TenantId UUID tenantId;   // tự filter khi đọc, tự set khi insert
  ...
}
```
+ `CurrentTenantIdentifierResolver` đọc từ SecurityContext.

### Postgres RLS

```sql
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders FORCE ROW LEVEL SECURITY;   -- áp cả với owner của table

CREATE POLICY tenant_isolation ON orders
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
```

### 🔴 Cái bẫy connection pool

```java
// ✅ SET LOCAL — tự reset khi transaction kết thúc
jdbc.execute("SET LOCAL app.tenant_id = '" + tenantId + "'");
```

Nếu dùng `SET` (không có `LOCAL`), giá trị bám vào **session** của connection. HikariCP trả connection về pool, request của tenant B mượn đúng connection đó và **vẫn mang `app.tenant_id` của tenant A**.

Rò rỉ dữ liệu im lặng — không log, không exception, không ai biết cho tới khi shop A nhìn thấy đơn của shop B.

`SET LOCAL` gắn với transaction nên tự sạch. **Bắt buộc.**

### Admin bypass

Đừng viết exception trong policy. Dùng **DataSource thứ hai** với DB role có `BYPASSRLS`, chỉ inject vào service admin. Tách vật lý thì không thể dùng nhầm.

### Test — viết trong tuần đầu, chạy trong CI

```java
@Test
void tenant_b_khong_doc_duoc_du_lieu_cua_a() {
    var a = seedTenantWithOrders(3);
    var b = seedTenantWithOrders(2);

    withTenant(b, () -> {
        assertThat(orderRepo.findAll()).hasSize(2);
        assertThat(orderRepo.findById(a.orderIds().get(0))).isEmpty();  // ← quan trọng nhất
    });
}
```

Dòng `findById` mới đáng giá: `findAll` bị filter là đương nhiên; **truy cập trực tiếp bằng ID mới là đường rò rỉ thật** — ID lộ qua URL, qua log, qua đoán.

Thêm ArchUnit rule cấm `nativeQuery = true` trong repository có tenant scope, trừ whitelist được review.

> 🔴 Rò rỉ tenant là lỗi mang tính sống còn. Shop A nhìn thấy đơn hoặc danh sách khách của shop B **một lần** là mất cả tỉnh đó — chủ quán ở tỉnh biết nhau hết.

---

## 3. Trạng thái đơn — hai trục, đừng trộn

```
order_status:    PENDING → CONFIRMED → PREPARING → READY → DELIVERING → COMPLETED
                    ↓          ↓            ↓         ↓          ↓
                CANCELLED  CANCELLED    CANCELLED  CANCELLED   FAILED

payment_status:  UNPAID | PAID | PARTIAL | REFUNDED
```

Hai trục **độc lập**. Lý do thực tế: khách chuyển khoản trước rồi quán mới xác nhận; khách trả tiền mặt khi nhận (PAID xảy ra ở bước DELIVERING); khách huỷ sau khi đã trả (CANCELLED + PAID → REFUNDED).

Nhồi vào một enum thì 3 tháng nữa bạn sẽ có `CANCELLED_BUT_PAID_AWAITING_REFUND`.

**Bảng chuyển trạng thái khai báo tường minh**, không rải `if` khắp service:

```java
static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
    PENDING,    Set.of(CONFIRMED, CANCELLED),
    CONFIRMED,  Set.of(PREPARING, CANCELLED),
    PREPARING,  Set.of(READY, CANCELLED),
    READY,      Set.of(DELIVERING, COMPLETED, CANCELLED),
    DELIVERING, Set.of(COMPLETED, FAILED)
);
```

**Mọi chuyển trạng thái ghi một dòng vào `order_events` (append-only, cùng transaction):**

```sql
order_events (id, tenant_id, order_id, from_status, to_status,
              actor_type, actor_id, reason, created_at)
```

Bảng này cho miễn phí: audit trail khi chủ quán cãi nhau với khách, dữ liệu tính "thời gian chuẩn bị trung bình", và khả năng debug một đơn cụ thể lúc 7h tối mà không đoán mò.

---

## 4. Tiền — ba luật không thương lượng

**a) `BIGINT`, đơn vị đồng.** VND không có phần thập phân → `long` chính xác tuyệt đối. Không `double`, không `BigDecimal`.

**b) Snapshot giá và tên vào `order_items`:**

```sql
order_items (
  id, order_id, tenant_id,
  product_id UUID NULL,         -- reference, null nếu món đã xoá
  name_snapshot TEXT NOT NULL,  -- tên LÚC ĐẶT
  unit_price BIGINT NOT NULL,   -- giá LÚC ĐẶT
  qty INT NOT NULL,
  options JSONB                 -- topping/size, snapshot cả giá
)
```

Quán tăng giá bún từ 35k lên 40k lúc 10h sáng. Nếu join sang `products`, toàn bộ đơn hôm qua đột nhiên hiển thị sai và doanh thu báo cáo lệch. **Lỗi này không sửa được sau khi đã mất dữ liệu gốc.**

**c) Soft delete cho `products`.** Chủ quán sẽ xoá món, và đơn cũ vẫn phải xem được.

---

## 5. Đối soát VietQR — bắt đầu bằng nút bấm

Vì tiền đi **thẳng vào tài khoản shop**, backend không nhìn thấy giao dịch trừ khi shop kết nối sao kê qua Casso/SePay. Đó là bước onboarding thêm mà nhiều chủ quán sẽ ngại.

### MVP đúng: nút "Đã nhận tiền" trong merchant app

Chủ quán nhìn app ngân hàng của mình, thấy tiền về, bấm nút. Xong.

Chạy được từ ngày đầu, không phụ thuộc bên thứ ba, không rủi ro. Tự động đối soát là tối ưu hoá cho shop đông đơn — làm ở giai đoạn 2.

### Khi làm tự động

- **Mã tham chiếu ngắn** (6–8 ký tự alphanumeric) trong nội dung CK — nhiều ngân hàng cắt nội dung dài
- **Webhook idempotent** theo transaction id của ngân hàng — nó sẽ gửi trùng
- **Lệch tiền → KHÔNG tự động PAID.** Thiếu 5k hay thừa 10k đều đẩy vào hàng đợi đối soát thủ công. Đoán mò ở đây là mất tiền thật của chủ quán.
- Webhook có thể đến **trước** khi đơn commit → lưu `payment_events` độc lập rồi match ngược, đừng vứt

> 🔒 **Nguyên tắc bất di bất dịch:** tiền không bao giờ chảy qua tài khoản của DynamicShop. Giữ nguyên tắc này thì không cần giấy phép trung gian thanh toán.

---

## 6. Idempotency — chống đơn trùng

Mạng 3G chập chờn, khách bấm "Đặt hàng" hai lần, quán làm hai tô.

```
POST /v1/s/{slug}/orders
Idempotency-Key: 8f14e45f-…     ← client sinh UUID, giữ nguyên khi retry
```

```sql
idempotency_keys (
  key TEXT, tenant_id UUID, request_hash TEXT,
  response_status INT, response_body JSONB, created_at,
  PRIMARY KEY (key, tenant_id)
)
```

- Trùng key + trùng hash → trả lại response cũ
- Trùng key + khác hash → `409 Conflict`
- TTL 24h rồi dọn

Áp dụng cho **mọi POST làm thay đổi tiền hoặc tạo thực thể**, không chỉ tạo đơn. Merchant app cũng dùng khi flush hàng đợi offline.

---

## 7. Outbox — đừng gọi FCM trong transaction

```sql
outbox (id, tenant_id, aggregate_id, type, payload JSONB,
        created_at, processed_at NULL, attempts INT DEFAULT 0)
```

Ghi vào outbox **trong cùng transaction** với việc tạo đơn. Một `@Scheduled` poll bảng này và gửi FCM.

**Vì sao bắt buộc:** nếu gọi FCM ngay trong service, có kịch bản FCM thành công rồi transaction rollback → **chuông kêu ở quán nhưng không có đơn nào trong hệ thống**. Chủ quán mất niềm tin ngay lần đầu.

At-least-once → client phải dedupe theo `order_id`.

---

## 8. Endpoint cho merchant polling

Merchant app poll mỗi 15–20 giây khi foreground (xem file 02). Thiết kế endpoint cho rẻ:

```
GET /v1/merchant/orders/sync?since=2026-07-29T10:15:00Z
→ { "orders": [...], "server_time": "...", "has_more": false }
```

- Trả delta theo `updated_at`, không trả toàn bộ
- Index `(tenant_id, updated_at)`
- ETag / `304 Not Modified` khi không có gì mới → phần lớn lần poll tốn ~200 byte

---

## 9. API storefront công khai — nơi dễ rò rỉ nhất

```
GET /v1/s/{slug}/storefront?schema=3
```

- Không auth, cache được, ETag
- Rate limit theo IP (Bucket4j đủ)
- **DTO/projection riêng cho public — tuyệt đối không serialize entity**

Điểm cuối là chỗ quan trọng: nếu trả thẳng entity `Product`, một ngày ai đó thêm field `costPrice` hoặc `supplierNote` và nó xuất hiện trong API công khai. **Giá vốn của quán bị lộ ra internet.**

Viết `PublicProductDto` với đúng field cần, và thêm test kiểm tra response JSON không chứa key nhạy cảm.

Server cũng chịu trách nhiệm **resolve mọi `data_ref`** trong layout để client chỉ cần một round-trip (xem file 01).

---

## 10. Múi giờ và "ngày kinh doanh"

Lưu UTC, hiển thị `Asia/Ho_Chi_Minh`. Nhưng: **quán ăn đêm đóng cửa 2h sáng — đơn lúc 1h sáng thuộc doanh thu ngày nào?**

Với chủ quán, đó là doanh thu của *tối hôm qua*. Cắt theo nửa đêm → báo cáo luôn sai → chủ quán không tin số liệu → mất niềm tin vào cả sản phẩm.

```sql
tenants (
  ...,
  timezone TEXT DEFAULT 'Asia/Ho_Chi_Minh',
  business_day_start TIME DEFAULT '04:00'
)
```

Mọi tổng hợp theo ngày dùng mốc này. Rẻ để làm bây giờ, đau đớn để sửa sau khi đã có báo cáo lịch sử.

---

## 11. Công nghệ tích hợp

| Công nghệ | Why | How |
|---|---|---|
| **Spring Boot 3.x + Java 21** | Virtual threads, records, pattern matching | Monolith, module hoá bằng package |
| **Postgres 16** | RLS, JSONB, đủ mạnh tới hàng chục nghìn đơn/ngày | Một instance, backup hằng ngày |
| **Hibernate 6 `@TenantId`** | Cô lập tenant ở tầng app | + `CurrentTenantIdentifierResolver` |
| **Postgres RLS** | Lưới an toàn cuối cùng | `SET LOCAL` trong mỗi transaction |
| **Flyway** | Migration có version, lặp lại được | `ddl-auto: validate`, **không bao giờ** `update` |
| **Spring Security + JWT** | Hai mặt phẳng public/authenticated | Audience khác nhau cho merchant / admin |
| **Firebase Admin SDK** | Gửi FCM cho merchant + customer | Gọi từ outbox worker, không từ service |
| **Bucket4j** | Rate limit endpoint công khai | In-memory là đủ ở 1 instance |
| **Casso / SePay** *(giai đoạn 2)* | Đọc biến động số dư để đối soát | Webhook + idempotent theo bank txn id |
| **Sentry** | Lỗi backend, trace xuyên tầng | Chung project với Next.js |
| **Testcontainers** | Test tenant isolation cần Postgres thật (RLS không mock được) | Chạy trong CI |
| **Docker Compose** | Deploy đơn giản trên 1 VPS | Spring + Postgres + Caddy |

### Logging: `tenant_id` trong MDC

```java
MDC.put("tenantId", ctx.tenantId().toString());
MDC.put("requestId", requestId);
```

Khi chủ quán gọi *"app em bị lỗi"*, bạn grep một phát ra ngay thay vì lục qua 200 shop. Rẻ, và giá trị tăng theo số lượng shop.

---

## 12. Migration phải expand/contract

App phiên bản cũ vẫn đang chạy trong lúc bạn deploy. Không bao giờ:

```
1. Thêm cột NULLABLE          → deploy
2. Backfill dữ liệu           → deploy code ghi cả cột mới
3. Đọc từ cột mới             → deploy
4. Bỏ cột cũ / thêm NOT NULL  → deploy (sau khi chắc chắn)
```

Bốn bước thay vì một. Chậm hơn, nhưng không có downtime và không hỏng app cũ trên máy khách.

---

## 13. Những thứ ĐỪNG làm

Bạn đang một mình hoặc team rất nhỏ. Mỗi thứ dưới đây nghe "chuyên nghiệp" nhưng sẽ ăn nhiều tuần mà không đổi lại gì ở quy mô 10–100 shop:

| Đừng | Thay bằng |
|---|---|
| Microservices | Monolith, tách module bằng package |
| Kafka / RabbitMQ | Bảng `outbox` + `@Scheduled` |
| CQRS / Event Sourcing | CRUD + bảng `order_events` |
| Redis (lúc đầu) | Caffeine in-memory; thêm Redis khi chạy nhiều instance |
| GraphQL | REST — client là của bạn, kiểm soát cả hai đầu |
| Kubernetes | Docker Compose trên 1 VPS |
| WebSocket | FCM + polling (xem file 02) |

**Nguyên tắc:** mọi thành phần hạ tầng thêm vào là một thứ có thể sập lúc 7h tối và bạn phải tự sửa. Giữ tối thiểu cho tới khi có doanh thu trả tiền cho sự phức tạp.

---

## 14. Hạ tầng & vận hành

```
1 VPS (Singapore, latency ~30–50ms về VN) + Docker Compose
  ├── Spring Boot
  ├── Postgres  (+ backup hằng ngày ra object storage)
  └── Caddy     (TLS tự động)
```

**CI/CD (GitHub Actions):**

| Trigger | Việc |
|---|---|
| PR | `./gradlew test` — **bao gồm test cô lập tenant** |
| Merge `develop` | Build image → deploy staging |
| Tag `v*` | Docker build → deploy VPS |

**Bắt buộc: diễn tập khôi phục backup.** Restore DB từ backup vào máy local **một lần** trước khi có shop thật. Backup chưa từng restore thành công thì không phải backup. Mất đơn hàng của một quán là mất quán đó vĩnh viễn.

Thêm: health check endpoint + UptimeRobot.

---

## 15. Thứ tự build

| Tuần | Việc |
|---|---|
| **1** | Postgres schema + Flyway + `tenant_id` mọi bảng + RLS + `SET LOCAL` + **test cô lập tenant chạy trong CI**. Chưa cần API nào. Đây là nền móng — đổ sai thì đập hết. |
| **2** | Auth (JWT, hai mặt phẳng) + CRUD sản phẩm/danh mục + API storefront public với DTO riêng |
| **3** | Đơn hàng: state machine + `order_events` + idempotency + snapshot giá |
| **4** | Outbox + FCM + endpoint polling cho merchant + nút "Đã nhận tiền" |

---

## Checklist

- [ ] `tenant_id` không bao giờ đến từ request param/body
- [ ] `SET LOCAL` (không phải `SET`) — đã kiểm tra bằng test dùng lại connection từ pool
- [ ] Test cô lập tenant có cả `findById`, chạy trong CI, fail build khi vi phạm
- [ ] DataSource admin tách riêng với role `BYPASSRLS`
- [ ] Tiền lưu `BIGINT`, không có `double` nào trong domain
- [ ] `order_items` snapshot `name` + `unit_price`
- [ ] `payment_status` tách khỏi `order_status`
- [ ] Mọi chuyển trạng thái ghi `order_events` cùng transaction
- [ ] Idempotency-Key trên mọi POST tạo thực thể
- [ ] FCM chỉ gửi từ outbox worker
- [ ] API public dùng DTO riêng + test không lộ field nhạy cảm
- [ ] `ddl-auto: validate`, Flyway quản mọi thay đổi schema
- [ ] `tenant_id` trong MDC của log
- [ ] **Đã restore backup thành công ít nhất một lần**
