-- V2 — device_tokens: nơi merchant_app đăng ký FCM registration token của máy.
-- Sprint 2.1 (progress.md mục 9). Bảng này là mảnh còn thiếu khiến OutboxBatchProcessor
-- luôn gọi fcmSender.send(null, ...) từ Stage 1 — không phải bug, chỉ chưa có consumer.
--
-- Không cần credential Firebase thật để dựng/kiểm chứng: shape của token do SDK FCM quy
-- định sẵn (chuỗi opaque), và toàn bộ đường ống "đơn mới → outbox → worker → resolve token
-- đúng tenant → gọi sender" test được offline bằng token giả + fake sender.

CREATE TABLE device_tokens (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id    UUID NOT NULL REFERENCES tenants (id),
  merchant_id  UUID NOT NULL REFERENCES merchants (id),
  token        TEXT NOT NULL,
  platform     TEXT NOT NULL CHECK (platform IN ('ANDROID', 'IOS')),
  app_version  TEXT,
  last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  revoked_at   TIMESTAMPTZ,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

  -- Unique THEO TENANT, không unique toàn cục. Cân nhắc có chủ ý, xem aware.md:
  -- unique toàn cục sẽ đúng hơn về mặt "một token = một máy = một chủ", nhưng app không
  -- thể tự dọn dòng của tenant khác (RLS chặn, và role BYPASSRLS chỉ được dùng trong
  -- package admin/) — insert sẽ chết bằng lỗi 23505 không diễn giải nổi cho người dùng.
  -- Cách dọn đúng cho token cũ là phản hồi UNREGISTERED của chính FCM, cần Firebase thật
  -- (sprint 2.5). Ghi nhận giới hạn thay vì giả vờ đã giải quyết.
  UNIQUE (tenant_id, token)
);

ALTER TABLE device_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_tokens FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON device_tokens
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- Truy vấn nóng nhất: outbox worker lấy mọi token còn sống của tenant hiện tại để fan-out
-- push. tenant_id đứng đầu theo checklist .claude/skills/tenant-isolation.
CREATE INDEX ON device_tokens (tenant_id) WHERE revoked_at IS NULL;
CREATE INDEX ON device_tokens (tenant_id, merchant_id);
