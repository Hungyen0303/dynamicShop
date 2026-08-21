---
name: flyway-migration
description: Dùng khi tạo hoặc sửa migration Flyway, thêm cột, thêm bảng, đổi schema Postgres. Chứa quy tắc expand/contract và checklist bảng mới cho multi-tenant.
---

# Flyway migration

`hibernate.ddl-auto: validate`. **Không bao giờ `update`.** Flyway quản mọi thay đổi schema.

## Expand / contract — bốn bước, không gộp

```
1. Thêm cột NULLABLE                         → deploy
2. Backfill + code ghi vào CẢ cột cũ và mới  → deploy
3. Code đọc từ cột mới                       → deploy
4. Bỏ cột cũ / thêm NOT NULL                 → deploy
```

App phiên bản cũ vẫn đang chạy trong lúc deploy, và app trên máy khách của chủ quán còn cũ hơn nữa — họ không cập nhật ngay.

## Cấm trong một migration
- `DROP COLUMN` cùng lúc với deploy code mới
- `ALTER COLUMN ... SET NOT NULL` chưa backfill
- Đổi tên cột → dùng thêm-mới, copy, bỏ-cũ
- `ALTER TYPE` trên bảng lớn khi đang có tải

## Checklist bảng mới
- [ ] `tenant_id UUID NOT NULL` (trừ bảng toàn cục: `users`, `tenants`, `admin_users`)
- [ ] `ENABLE ROW LEVEL SECURITY` + `FORCE` + policy `tenant_isolation`
- [ ] `created_at`, và `updated_at` nếu bảng có sửa
- [ ] Cột tiền là `BIGINT` (đơn vị đồng, VND không có thập phân)
- [ ] Index có `tenant_id` ở vị trí đầu
- [ ] Đã thêm vào test cô lập tenant
- [ ] Soft delete nếu bị tham chiếu bởi dữ liệu lịch sử (ví dụ `products` ← `order_items`)

## Đặt tên
`V{số}__{mô_tả_ngắn}.sql` — ví dụ `V7__them_business_day_start.sql`

**Migration đã merge thì không bao giờ sửa.** Viết migration mới.

## Nhớ
Snapshot giá trong `order_items` là chủ ý, không phải thiếu chuẩn hoá. Nếu một migration định bỏ `name_snapshot` / `unit_price` để "chuẩn hoá dữ liệu" — từ chối và hỏi người.
