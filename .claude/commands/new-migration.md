---
description: Tạo migration Flyway mới
argument-hint: <mô_tả_ngắn>
---

Tạo migration: **$1**

Đọc `docs/31-database.md` mục "Migration — expand/contract" trước.

## Quy tắc bốn bước — không được gộp
```
1. Thêm cột NULLABLE                         → deploy
2. Backfill + code ghi vào CẢ cột cũ và mới  → deploy
3. Code đọc từ cột mới                       → deploy
4. Bỏ cột cũ / thêm NOT NULL                 → deploy
```
App phiên bản cũ vẫn đang chạy trong lúc deploy, và app trên máy khách còn cũ hơn nữa.

## Cấm trong một migration
- `DROP COLUMN` cùng lúc với deploy code mới
- `SET NOT NULL` mà chưa backfill
- Đổi tên cột (dùng thêm-mới → copy → bỏ-cũ)
- `ALTER TYPE` trên bảng lớn khi đang có tải

## Checklist bảng mới
- [ ] `tenant_id UUID NOT NULL` (trừ bảng toàn cục — cần lý do rõ)
- [ ] `ENABLE ROW LEVEL SECURITY` + `FORCE` + policy `tenant_isolation`
- [ ] `created_at`, và `updated_at` nếu bảng có sửa
- [ ] Cột tiền là `BIGINT`
- [ ] Index có `tenant_id` ở vị trí đầu
- [ ] Đã thêm vào test cô lập tenant
- [ ] Nếu bị tham chiếu bởi dữ liệu lịch sử ⇒ soft delete

Đặt tên `V{số}__{mô_tả_ngắn}.sql`. Migration đã merge thì **không bao giờ sửa** — viết cái mới.
