-- V1 — bảng lõi (docs/31-database.md mục "Bảng lõi" + "Index tối thiểu").
-- Chạy bằng role postgres (superuser) — xem spring.flyway.* trong application-local.yml.
-- app_user / app_admin chỉ có USAGE trên schema public, không CREATE — đúng thiết kế.
--
-- RLS: mọi bảng có tenant_id đều ENABLE + FORCE ROW LEVEL SECURITY + policy tenant_isolation,
-- dùng current_setting('app.tenant_id', true)::uuid. FORCE bắt buộc vì owner (postgres)
-- mới là role tạo bảng — không có FORCE thì owner tự động bỏ qua policy, và vì Flyway
-- luôn chạy bằng postgres nên bảng sẽ luôn "owned by" role đó.
--
-- Ngoại lệ không có tenant_id: tenants (bảng gốc), users (customer toàn cục).

-- ============================================================
-- tenants — bảng gốc, không có tenant_id, không RLS
-- ============================================================
CREATE TABLE tenants (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  slug                 TEXT NOT NULL UNIQUE,
  name                 TEXT NOT NULL,
  status               TEXT NOT NULL DEFAULT 'ACTIVE',
  timezone             TEXT NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
  business_day_start   TIME NOT NULL DEFAULT '04:00',
  -- mô hình bán đứt: hạn mức dung lượng (Stage 3 dùng, nhưng tạo cột từ V1)
  storage_quota_bytes  BIGINT NOT NULL DEFAULT 2147483648,
  storage_used_bytes   BIGINT NOT NULL DEFAULT 0,
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- users — customer, TOÀN CỤC, không thuộc tenant nào, không RLS
-- ============================================================
CREATE TABLE users (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  phone      TEXT NOT NULL UNIQUE,
  zalo_id    TEXT UNIQUE,
  name       TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- merchants — người của shop, thuộc đúng một tenant
-- ============================================================
CREATE TABLE merchants (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id     UUID NOT NULL REFERENCES tenants (id),
  phone         TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  name          TEXT NOT NULL,
  role          TEXT NOT NULL DEFAULT 'OWNER',
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, phone)
);

ALTER TABLE merchants ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchants FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON merchants
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE INDEX ON merchants (tenant_id);

-- ============================================================
-- categories
-- ============================================================
CREATE TABLE categories (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id  UUID NOT NULL REFERENCES tenants (id),
  name       TEXT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at TIMESTAMPTZ
);

ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON categories
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE INDEX ON categories (tenant_id);

-- ============================================================
-- products — giá là BIGINT, đơn vị đồng. Soft delete (order_items tham chiếu).
-- ============================================================
CREATE TABLE products (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id   UUID NOT NULL REFERENCES tenants (id),
  category_id UUID REFERENCES categories (id),
  name        TEXT NOT NULL,
  price       BIGINT NOT NULL CHECK (price >= 0),
  image_url   TEXT,
  available   BOOLEAN NOT NULL DEFAULT true,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at  TIMESTAMPTZ
);

ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE products FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON products
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE INDEX ON products (tenant_id, category_id) WHERE deleted_at IS NULL;

-- ============================================================
-- orders — order_status và payment_status TÁCH RỜI, hai trục độc lập.
-- Giá trị khớp contracts/order-states.json.
-- ============================================================
CREATE TABLE orders (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id        UUID NOT NULL REFERENCES tenants (id),
  code             TEXT NOT NULL,
  user_id          UUID REFERENCES users (id),
  order_status     TEXT NOT NULL DEFAULT 'PENDING'
    CHECK (order_status IN ('PENDING','CONFIRMED','PREPARING','READY','DELIVERING','COMPLETED','CANCELLED','FAILED')),
  payment_status   TEXT NOT NULL DEFAULT 'UNPAID'
    CHECK (payment_status IN ('UNPAID','PAID','PARTIAL','REFUNDED')),
  subtotal         BIGINT NOT NULL CHECK (subtotal >= 0),
  shipping_fee     BIGINT NOT NULL DEFAULT 0 CHECK (shipping_fee >= 0),
  discount         BIGINT NOT NULL DEFAULT 0 CHECK (discount >= 0),
  total            BIGINT NOT NULL CHECK (total >= 0),
  note             TEXT,
  delivery_address TEXT,
  phone            TEXT,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, code)
);

ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON orders
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- (tenant_id, updated_at) là index quan trọng nhất — endpoint sync merchant dựa vào nó
CREATE INDEX ON orders (tenant_id, updated_at);
CREATE INDEX ON orders (tenant_id, created_at);

-- ============================================================
-- order_items — SNAPSHOT name_snapshot + unit_price. Không join products để hiển thị đơn cũ.
-- ============================================================
CREATE TABLE order_items (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id       UUID NOT NULL REFERENCES orders (id),
  tenant_id      UUID NOT NULL REFERENCES tenants (id),
  product_id     UUID REFERENCES products (id),
  name_snapshot  TEXT NOT NULL,
  unit_price     BIGINT NOT NULL CHECK (unit_price >= 0),
  qty            INT NOT NULL CHECK (qty > 0),
  options        JSONB,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON order_items
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE INDEX ON order_items (order_id);

-- ============================================================
-- order_events — APPEND ONLY. Mọi chuyển trạng thái ghi một dòng, cùng transaction.
-- ============================================================
CREATE TABLE order_events (
  id          BIGSERIAL PRIMARY KEY,
  tenant_id   UUID NOT NULL REFERENCES tenants (id),
  order_id    UUID NOT NULL REFERENCES orders (id),
  from_status TEXT,
  to_status   TEXT NOT NULL,
  actor_type  TEXT NOT NULL,
  actor_id    UUID,
  reason      TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE order_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_events FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON order_events
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE INDEX ON order_events (tenant_id, order_id, created_at);

-- ============================================================
-- payment_events — độc lập với orders. tenant_id NULL nếu chưa match được webhook.
-- ============================================================
CREATE TABLE payment_events (
  id               BIGSERIAL PRIMARY KEY,
  tenant_id        UUID REFERENCES tenants (id),
  bank_txn_id      TEXT NOT NULL UNIQUE,
  amount           BIGINT NOT NULL,
  reference_code   TEXT,
  matched_order_id UUID REFERENCES orders (id),
  raw              JSONB,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS vẫn bật dù tenant_id có thể NULL lúc chưa match — policy cho qua NULL
-- (chưa match thì chưa thuộc tenant nào, chỉ package payment/ đọc trực tiếp trước khi match).
ALTER TABLE payment_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_events FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON payment_events
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE INDEX ON payment_events (tenant_id);

-- ============================================================
-- storefronts — layout (screens) và theme tách cột, PK = tenant_id (1-1)
-- ============================================================
CREATE TABLE storefronts (
  tenant_id      UUID PRIMARY KEY REFERENCES tenants (id),
  schema_version INT NOT NULL,
  layout         JSONB NOT NULL,
  theme          JSONB NOT NULL,
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE storefronts ENABLE ROW LEVEL SECURITY;
ALTER TABLE storefronts FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON storefronts
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- ============================================================
-- outbox — worker Stage 0 chỉ ghi log, chưa nối FCM
-- ============================================================
CREATE TABLE outbox (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    UUID NOT NULL REFERENCES tenants (id),
  aggregate_id UUID NOT NULL,
  type         TEXT NOT NULL,
  payload      JSONB NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  processed_at TIMESTAMPTZ,
  attempts     INT NOT NULL DEFAULT 0
);

ALTER TABLE outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE outbox FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON outbox
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE INDEX ON outbox (processed_at) WHERE processed_at IS NULL;

-- ============================================================
-- idempotency_keys — khoá kép (key, tenant_id). Vẫn bật RLS trên tenant_id.
-- ============================================================
CREATE TABLE idempotency_keys (
  key             TEXT NOT NULL,
  tenant_id       UUID NOT NULL REFERENCES tenants (id),
  request_hash    TEXT NOT NULL,
  response_status INT,
  response_body   JSONB,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (key, tenant_id)
);

ALTER TABLE idempotency_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE idempotency_keys FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON idempotency_keys
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- ============================================================
-- audit_logs — hành động operator/impersonate. Không có tenant_id cố định
-- (một operator có thể tác động nhiều tenant); target_tenant_id chỉ để tra cứu,
-- không dùng làm cột RLS vì bảng này chỉ package admin/ (BYPASSRLS) ghi/đọc.
-- ============================================================
CREATE TABLE audit_logs (
  id               BIGSERIAL PRIMARY KEY,
  admin_user_id    UUID NOT NULL,
  action           TEXT NOT NULL,
  target_tenant_id UUID REFERENCES tenants (id),
  detail           JSONB,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ON audit_logs (target_tenant_id);

-- ============================================================
-- media — ảnh/video món ăn, tính vào quota tenant
-- ============================================================
CREATE TABLE media (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id  UUID NOT NULL REFERENCES tenants (id),
  kind       TEXT NOT NULL CHECK (kind IN ('image', 'video')),
  path       TEXT NOT NULL,
  size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
  width      INT,
  height     INT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at TIMESTAMPTZ
);

ALTER TABLE media ENABLE ROW LEVEL SECURITY;
ALTER TABLE media FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON media
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE INDEX ON media (tenant_id);
