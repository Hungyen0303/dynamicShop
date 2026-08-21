# 31 — Database

Postgres 16, chạy qua Docker local ở Stage 0. Đọc kèm `00-context.md`, `70-stages.md`, `30-backend.md`.

---

## Bất biến

1. **Mọi bảng nghiệp vụ có `tenant_id UUID NOT NULL`** và bật RLS. Ngoại lệ duy nhất: bảng toàn cục (`users`, `tenants`, `admin_users`, `idempotency_keys` có `tenant_id` nhưng là khoá kép).
2. **`SET LOCAL app.tenant_id`**, không bao giờ `SET`.
3. **Tiền lưu `BIGINT`**, đơn vị đồng.
4. **Flyway quản mọi thay đổi.** `hibernate.ddl-auto: validate`, không bao giờ `update`.
5. **Migration phải expand/contract.** Không bao giờ một bước phá vỡ tương thích.
6. **Không xoá cứng `products`** — soft delete, vì `order_items` tham chiếu.
7. **Không xoá dòng `order_events`.** Append-only.

---

## 🔴 Cái bẫy connection pool — đọc kỹ

```java
// ✅ ĐÚNG — gắn với transaction, tự reset
jdbc.execute("SET LOCAL app.tenant_id = '" + tenantId + "'");

// ☠️ SAI — gắn với session của connection
jdbc.execute("SET app.tenant_id = '" + tenantId + "'");
```

Với `SET` (không `LOCAL`), giá trị bám vào session. HikariCP trả connection về pool; request của tenant B mượn đúng connection đó và **vẫn mang `app.tenant_id` của tenant A**.

Rò rỉ dữ liệu **im lặng** — không log, không exception. Không ai biết cho tới khi shop A nhìn thấy đơn của shop B.

Có test riêng cho tình huống này: mượn connection, đổi tenant, kiểm tra GUC đã sạch.

---

## RLS

```sql
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders FORCE ROW LEVEL SECURITY;   -- áp cả với owner của table

CREATE POLICY tenant_isolation ON orders
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
```

`FORCE` là bắt buộc — không có nó, role owner (thường là role app dùng) bỏ qua policy.

**Hai tầng:**
```
Hibernate @TenantId  →  nhanh, tự động, app-level   (cứu 99%)
Postgres RLS         →  người từ chối CUỐI CÙNG      (cứu 1% còn lại)
```

### Admin bypass

Đừng viết exception vào policy. Dùng **DataSource thứ hai** với DB role có `BYPASSRLS`, chỉ inject vào package `admin/`. Tách vật lý thì không thể dùng nhầm.

```
app_user    — role thường, chịu RLS       → dùng cho public + merchant
app_admin   — role BYPASSRLS              → chỉ dùng trong admin/
```

---

## Bảng lõi

```sql
tenants (
  id UUID PK, slug TEXT UNIQUE, name TEXT,
  status TEXT,
  timezone TEXT DEFAULT 'Asia/Ho_Chi_Minh',
  business_day_start TIME DEFAULT '04:00',
  -- mô hình bán đứt: hạn mức dung lượng (Stage 3, nhưng tạo cột từ V1)
  storage_quota_bytes BIGINT DEFAULT 2147483648,   -- mặc định 2GB
  storage_used_bytes  BIGINT DEFAULT 0,
  created_at, updated_at
)

users (                      -- customer, TOÀN CỤC, không có tenant_id
  id UUID PK, phone TEXT UNIQUE, zalo_id TEXT UNIQUE NULL,
  name TEXT, created_at
)

merchants (                  -- người của shop
  id UUID PK, tenant_id UUID NOT NULL,
  phone TEXT, password_hash TEXT, role TEXT,
  UNIQUE (tenant_id, phone)
)

categories (id, tenant_id, name, sort_order, deleted_at NULL)

products (
  id UUID PK, tenant_id UUID NOT NULL, category_id UUID,
  name TEXT, price BIGINT NOT NULL,       -- ĐỒNG
  image_url TEXT, available BOOLEAN,
  deleted_at TIMESTAMPTZ NULL,             -- soft delete
  created_at, updated_at
)

orders (
  id UUID PK, tenant_id UUID NOT NULL,
  code TEXT NOT NULL,                      -- mã ngắn cho khách, unique per tenant
  user_id UUID NULL,                       -- customer toàn cục, null nếu khách vãng lai
  order_status TEXT NOT NULL,
  payment_status TEXT NOT NULL,            -- TÁCH RỜI order_status
  subtotal BIGINT, shipping_fee BIGINT, discount BIGINT, total BIGINT,
  note TEXT, delivery_address TEXT, phone TEXT,
  created_at, updated_at,
  UNIQUE (tenant_id, code)
)

order_items (
  id UUID PK, order_id UUID, tenant_id UUID NOT NULL,
  product_id UUID NULL,                    -- null nếu món đã xoá
  name_snapshot TEXT NOT NULL,             -- tên LÚC ĐẶT
  unit_price BIGINT NOT NULL,              -- giá LÚC ĐẶT
  qty INT NOT NULL,
  options JSONB                            -- topping/size, snapshot cả giá
)

order_events (                             -- APPEND ONLY
  id BIGSERIAL PK, tenant_id UUID NOT NULL, order_id UUID,
  from_status TEXT, to_status TEXT,
  actor_type TEXT, actor_id UUID, reason TEXT,
  created_at
)

payment_events (                           -- độc lập với orders
  id BIGSERIAL PK, tenant_id UUID NULL,    -- null nếu chưa match được
  bank_txn_id TEXT UNIQUE,                 -- idempotency của webhook
  amount BIGINT, reference_code TEXT,
  matched_order_id UUID NULL,
  raw JSONB, created_at
)

storefronts (
  tenant_id UUID PK, schema_version INT,
  layout JSONB NOT NULL, theme JSONB NOT NULL,
  updated_at
)

outbox (
  id BIGSERIAL PK, tenant_id UUID, aggregate_id UUID,
  type TEXT, payload JSONB,
  created_at, processed_at TIMESTAMPTZ NULL, attempts INT DEFAULT 0
)

idempotency_keys (
  key TEXT, tenant_id UUID, request_hash TEXT,
  response_status INT, response_body JSONB, created_at,
  PRIMARY KEY (key, tenant_id)
)

audit_logs (                               -- cho impersonate và hành động admin
  id BIGSERIAL PK, admin_user_id UUID, action TEXT,
  target_tenant_id UUID NULL, detail JSONB, created_at
)

media (                                    -- ảnh/video món ăn, tính vào quota
  id UUID PK, tenant_id UUID NOT NULL,
  kind TEXT,                               -- image | video
  path TEXT, size_bytes BIGINT NOT NULL,
  width INT, height INT,
  created_at, deleted_at TIMESTAMPTZ NULL
)
```

---

## Vì sao snapshot giá — đừng "tối ưu" đi

```sql
order_items.name_snapshot   -- KHÔNG join products.name
order_items.unit_price      -- KHÔNG join products.price
```

Quán tăng giá bún từ 35k lên 40k lúc 10h sáng. Nếu join, **toàn bộ đơn hôm qua đột nhiên hiển thị sai** và doanh thu báo cáo lệch.

Lỗi này **không sửa được sau khi đã mất dữ liệu gốc**. Nếu thấy một PR bỏ snapshot để "chuẩn hoá dữ liệu", từ chối.

---

## Index tối thiểu

```sql
CREATE INDEX ON orders (tenant_id, updated_at);       -- endpoint sync của merchant
CREATE INDEX ON orders (tenant_id, created_at);       -- báo cáo theo ngày
CREATE INDEX ON order_items (order_id);
CREATE INDEX ON products (tenant_id, category_id) WHERE deleted_at IS NULL;
CREATE INDEX ON outbox (processed_at) WHERE processed_at IS NULL;
CREATE INDEX ON order_events (tenant_id, order_id, created_at);
```

`(tenant_id, updated_at)` là index quan trọng nhất — endpoint bị gọi nhiều nhất hệ thống dựa vào nó.

---

## Migration — expand/contract, bốn bước

Không bao giờ làm một bước. App phiên bản cũ vẫn đang chạy trong lúc deploy, và app trên máy khách còn cũ hơn nữa.

```
1. Thêm cột NULLABLE                         → deploy
2. Backfill + code ghi vào CẢ cột cũ và mới  → deploy
3. Code đọc từ cột mới                       → deploy
4. Bỏ cột cũ / thêm NOT NULL                 → deploy (sau khi chắc chắn)
```

**Cấm trong một migration:**
- `DROP COLUMN` cùng lúc với deploy code mới
- `ALTER COLUMN ... SET NOT NULL` mà không backfill trước
- Đổi tên cột (dùng thêm-mới → copy → bỏ-cũ)
- `ALTER TYPE` trên bảng lớn khi đang có tải

Đặt tên: `V{số}__{mô_tả_ngắn}.sql`. Migration đã merge thì **không bao giờ sửa** — viết migration mới.

---

## Checklist thêm bảng mới

- [ ] Có `tenant_id UUID NOT NULL` (trừ bảng toàn cục — cần lý do rõ)
- [ ] `ENABLE ROW LEVEL SECURITY` + `FORCE` + policy `tenant_isolation`
- [ ] Có `created_at`, và `updated_at` nếu bảng có sửa
- [ ] Cột tiền là `BIGINT`
- [ ] Đã thêm vào test cô lập tenant
- [ ] Index có `tenant_id` ở vị trí đầu
- [ ] Nếu bị tham chiếu bởi dữ liệu lịch sử → soft delete, không xoá cứng

---

## Mock data cho Stage 0

Hai tenant giả lập, nạp bằng Flyway migration riêng chỉ chạy ở profile `local`:

```
V900__mock_tenant_a.sql    quán bún — theme đỏ, 3 danh mục, 12 món
V901__mock_tenant_b.sql    quán trà sữa — theme xanh, 2 danh mục, 8 món
```

Hai shop phải **khác nhau rõ rệt về theme và layout** — đó là cách duy nhất chứng minh multi-tenant + SDUI thực sự hoạt động chứ không phải trùng hợp.

Đặt số hiệu từ V900 để không đụng dãy migration thật. Cấu hình Flyway chỉ nạp chúng khi `spring.profiles.active=local`.

---

## Backup

- Backup hằng ngày ra object storage, giữ 30 ngày
- **Đã restore thành công ít nhất một lần** trước khi có shop thật

Backup chưa từng restore thành công thì không phải backup. Mất đơn hàng của một quán là mất quán đó vĩnh viễn.

---

## Không làm

- Không dùng `SELECT *` trong code — cột mới sẽ lọt vào response
- Không `nativeQuery` trong repository có tenant scope (cần duyệt + test riêng)
- Không tạo view hoặc function bỏ qua RLS
- Không lưu số điện thoại đầy đủ trong log hoặc bảng phân tích
- Không thêm bảng cho tính năng chưa được duyệt (hỏi PM)
