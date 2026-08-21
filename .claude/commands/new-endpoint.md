---
description: Thêm một HTTP endpoint mới
argument-hint: <METHOD> <đường/dẫn>
---

Thêm endpoint **$1 $2**.

## Các bước — đúng thứ tự
1. Sửa `contracts/openapi.yaml` **trước**
2. `make generate` → sinh interface Java + client Dart + type TS
3. Xác định mặt phẳng:
   - Public: `/v1/s/{slug}/…` — tenant từ slug, không auth
   - Merchant: `/v1/merchant/…` — tenant từ JWT claim
   - Admin: `/v1/admin/…` — DataSource BYPASSRLS + audit log
4. Implement trong đúng package (`docs/30-backend.md` mục "Hình dạng")
5. Nếu public ⇒ viết DTO riêng, **không serialize entity**
6. Nếu POST tạo thực thể hoặc đụng tiền ⇒ thêm `Idempotency-Key`
7. Viết test, **bắt buộc có trường hợp cross-tenant**

## Kiểm tra bắt buộc
- [ ] `tenant_id` không đến từ request param/body
- [ ] Test cô lập tenant bao phủ endpoint mới, có `findById`
- [ ] Public endpoint: có rate limit + test không lộ field nhạy cảm
- [ ] `make verify-contracts` xanh

Sau đó chạy subagent `security-reviewer`.
