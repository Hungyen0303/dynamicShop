---
description: Thêm một block type mới vào SDUI
argument-hint: <tên_block>
---

Thêm block **$1** vào SDUI.

⚠️ **Cần người duyệt trước** (`AGENTS.md` mục 7). Xác nhận đã được duyệt trước khi làm.

Ở Stage 0, 8 block có sẵn trong `contracts/blocks.registry.json` là đủ để chứng minh SDUI hoạt động. Đừng thêm block mới trừ khi có lý do rõ ràng.

## Các bước — đúng thứ tự
1. Thêm entry vào `contracts/blocks.registry.json`: `type`, `props` (mỗi prop **có default**), `overridable` whitelist
2. `make generate`
3. Tạo widget trong `packages/ds_blocks/lib/blocks/` — pure, không network, chỉ đọc token từ context
4. Đăng ký trong registry của `ds_sdui`
5. Thêm fixture vào `test/fixtures/layouts/` + golden test
6. *(Stage 4)* Kiểm tra `studio_web` render đúng — bỏ qua ở Stage 0, studio chưa tồn tại
7. `make verify-contracts && make test && make lint`

## Kiểm tra bắt buộc
- [ ] Mọi prop có default — config cũ thiếu field mới vẫn parse được
- [ ] Không hardcode màu/bo góc/spacing
- [ ] Không import `dart:io` hay plugin chỉ có trên mobile
- [ ] Golden test pass ở `customer_app` (studio_web: Stage 4)
