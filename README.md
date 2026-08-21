# DynamicShop

App bán hàng riêng cho từng quán ăn nhỏ ở tỉnh. Shop tự có khách, tiền về thẳng tài khoản shop.
Mô hình: **bán đứt app** + hạn mức dung lượng; bảo trì và cập nhật tính phí riêng.

**Không phải sàn.** Không xếp hạng quán, không bán vị trí hiển thị, không điều phối đơn.

---

## 🟢 Trạng thái: Stage 0 — Local

Mọi thứ chạy trên máy local với 2 mock shop. Chưa deploy, chưa Firebase, chưa merchant app.
Xem **`docs/70-stages.md`** để biết việc gì được làm bây giờ.

## 🤖 Nếu bạn là AI agent

1. Đọc **`AGENTS.md`** — luật, bất biến, nguồn sự thật
2. Đọc **`docs/00-context.md`** — sản phẩm, thuật ngữ
3. Đọc **`docs/70-stages.md`** — 🔴 việc gì được làm bây giờ
4. Đọc doc miền của task đang làm
5. Nếu đang khởi tạo: đọc **`INIT.md`**

**Hai luật:** thiếu thông tin gì thì **hỏi người, đừng đoán**. Task thuộc stage sau thì **dừng, đừng làm**.

---

## Bản đồ

```
AGENTS.md            luật cho agent (CLAUDE.md, .cursorrules trỏ về đây)
INIT.md              hướng dẫn khởi tạo dự án
contracts/           NGUỒN SỰ THẬT — máy đọc được, sinh ra code
docs/                ràng buộc + cách làm
  00-context.md        đọc trước, luôn luôn
  10/11-*.md           customer app / merchant app
  20-web.md            Next.js admin + Flutter studio
  30/31-*.md           backend / database
  40-pm.md             phạm vi, ưu tiên
  50-qa.md             test, drift guard
  60-design.md         token, component
  70-stages.md         🔴 GIAI ĐOẠN — đọc trước mọi task
  80-git-workflow.md   monorepo, sparse checkout
  business/            chiến lược và tech spec đầy đủ (bối cảnh, lý do)
.claude/
  agents/              pm, po, qa, designer, engineer, security-reviewer, tech-writer
  commands/            /init-part /new-block /new-endpoint /new-migration /verify /review-pr /ask-owner
  skills/              tenant-isolation, sdui-block, flyway-migration, vn-market-constraints
backend/             Spring Boot monolith
apps/
  customer_app/        Flutter — app khách
  merchant_app/        Flutter — Android trước
  studio_web/          Flutter web — nhúng vào admin
packages/            ds_tokens, ds_components, ds_blocks, ds_sdui, ds_api, ds_core
web/admin/           Next.js — nội bộ
infra/docker/        Postgres
tools/seed/          dựng storefront hàng loạt
```

---

## Bắt đầu (Stage 0)

```bash
cd infra/docker && cp .env.example .env && docker compose up -d
cd ../../backend && ./gradlew test
```

Chưa init? Xem `INIT.md`.

---

## Mười bất biến

Chi tiết ở `AGENTS.md` mục 2. Tóm tắt:

1. `tenant_id` không bao giờ từ client input
2. `SET LOCAL`, không bao giờ `SET`
3. Tiền là `long`, đơn vị đồng
4. `order_items` snapshot tên + giá
5. `payment_status` tách rời `order_status`
6. Mọi transition ghi `order_events` cùng transaction
7. FCM chỉ gửi từ outbox
8. API public trả DTO riêng
9. Package dùng chung không hardcode style
10. Tiền không bao giờ chảy qua tài khoản DynamicShop

Cộng thêm: **menu/giá/theme tải runtime, không nhồi vào binary** — nếu không, đổi giá một món là phải submit store lại.
