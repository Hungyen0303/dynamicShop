# AGENTS.md

**Đây là file gốc cho mọi AI coding agent làm việc trên repo này.**
`CLAUDE.md`, `.cursorrules`, `.github/copilot-instructions.md` đều chỉ trỏ về file này. Sửa ở đây, không sửa ở đó.

---

## 0. Đọc gì cho task nào

Đừng đọc hết. Context là tài nguyên hữu hạn — đọc hai doc bắt buộc + đúng một doc miền.

| Task đụng tới | Đọc |
|---|---|
| **Bất cứ thứ gì** | `docs/00-context.md` **và** `docs/70-stages.md` (bắt buộc, luôn luôn) |
| Git, branch, commit, monorepo | `docs/80-git-workflow.md` |
| Customer app, block, SDUI, theme | `docs/10-customer-app.md` |
| Merchant app, đơn, in bill, chuông | `docs/11-merchant-app.md` |
| Next.js admin, Flutter studio | `docs/20-web.md` |
| Spring Boot, API, business logic | `docs/30-backend.md` |
| Schema, migration, query, RLS | `docs/31-database.md` |
| Phạm vi, ưu tiên, "có nên làm không" | `docs/40-pm.md` |
| Viết test, CI, drift guard | `docs/50-qa.md` |
| Token, spacing, component | `docs/60-design.md` |

---

## 0b. 🔴 Kiểm tra STAGE trước khi làm bất cứ việc gì

Dự án chia giai đoạn. **Rất nhiều thứ trong repo này cố ý chưa làm.**

Hiện tại: **Stage 0 — mọi thứ chạy local, 2 mock shop, không dịch vụ ngoài.**

Chỉ backend và customer_app được làm bây giờ. Những thứ sau **KHÔNG** làm dù thấy thư mục trống:

```
CI/CD · in bill nhiệt · seed script · melos · merchant_app
Next.js admin · studio_web · Firebase/FCM · VPS/deploy
đối soát tự động · Zalo (hoãn vô thời hạn)
```

Tra bảng đầy đủ ở `docs/70-stages.md`. Task thuộc stage sau ⇒ **dừng, nói rõ nó ở stage nào, hỏi người.**

---

## 1. Nguồn sự thật

**Code không bao giờ là nguồn sự thật cho contract. `/contracts/` mới là.**

| File | Định nghĩa | Sinh ra |
|---|---|---|
| `contracts/openapi.yaml` | Mọi HTTP endpoint | Client Dart + type TS |
| `contracts/storefront.schema.json` | Cấu trúc JSON storefront | Validator ở BE + FE |
| `contracts/blocks.registry.json` | Danh sách block, props, whitelist override | Registry Dart + form studio |
| `contracts/tokens.json` | Design token | `ds_tokens` (Dart) + `tokens.ts` |
| `contracts/order-states.json` | State machine đơn hàng | Enum + bảng transition |

**Luật:** sửa hành vi ⇒ sửa contract **trước**, chạy `make generate`, rồi mới sửa code. Không bao giờ sửa file đã sinh ra bằng tay — chúng có header `// GENERATED — DO NOT EDIT`.

CI có bước `make verify-contracts`. Nếu code lệch contract, build fail. Đây là cơ chế duy nhất giữ cho docs không nói dối.

---

## 2. Bất biến — vi phạm là chặn merge

Mười điều này áp cho **mọi** phần của hệ thống. Nếu một yêu cầu buộc phải vi phạm, **dừng lại và hỏi người**, đừng tự quyết.

1. `tenant_id` **không bao giờ** đến từ request param hoặc body. Chỉ từ JWT claim hoặc slug trong route.
2. Dùng `SET LOCAL app.tenant_id`, **không bao giờ** `SET` (connection pool sẽ rò rỉ giữa các tenant).
3. Tiền lưu bằng `BIGINT` / `long`, đơn vị đồng. Không `double`, không `float`.
4. `order_items` lưu **snapshot** `name` và `unit_price`. Không join sang `products` để hiển thị đơn cũ.
5. `payment_status` tách rời `order_status`. Không gộp thành một enum.
6. Mọi chuyển trạng thái đơn ghi một dòng `order_events` **trong cùng transaction**.
7. FCM chỉ gửi từ outbox worker, không bao giờ gọi trong transaction nghiệp vụ.
8. API công khai trả **DTO riêng**, không serialize entity.
9. Package dùng chung (`ds_blocks`, `ds_components`) **không hardcode** màu / bo góc / spacing — chỉ đọc token.
9b. **Menu, giá, theme, layout tải lúc runtime** — không bao giờ nhồi vào binary. Flavor chỉ chứa danh tính (tên app, icon, tenant mặc định).
10. Tiền **không bao giờ** chảy qua tài khoản DynamicShop. Chỉ từ khách sang shop.

---

## 3. Bản đồ repo

```
contracts/          nguồn sự thật, máy đọc được
docs/               ràng buộc + cách làm (file này + 9 doc)
packages/
  ds_tokens/        SINH RA từ contracts/tokens.json
  ds_components/    DsButton, DsInputBar, DsCard…
  ds_blocks/        widget block SDUI
  ds_sdui/          registry + renderer
  ds_api/           SINH RA từ contracts/openapi.yaml
  ds_core/          routing, storage, utils
apps/
  customer_app/     Flutter — app khách
  merchant_app/     Flutter — Android trước
  studio_web/       Flutter web — nhúng vào admin
web/admin/          Next.js — nội bộ
backend/            Spring Boot monolith
tools/              seed script, generator
```

---

## 4. Lệnh

```bash
make generate          # sinh code từ contracts/
make verify-contracts  # fail nếu code lệch contract
make test              # toàn bộ test
make lint              # gồm cả check hardcode style

# theo phần
melos run test:flutter
cd backend && ./gradlew test
cd web/admin && pnpm test
```

Trước khi báo "xong", chạy `make lint && make test && make verify-contracts`. Không tự nhận hoàn thành nếu chưa chạy.

---

## 5. Quy trình làm việc

**Trước khi viết code**
1. Đọc `00-context.md` + doc miền liên quan
2. Nếu task đụng contract → sửa contract trước, `make generate`
3. Nếu task mâu thuẫn với mục 2 (Bất biến) → **dừng và hỏi**

**Khi viết code**
- Theo pattern có sẵn trong cùng thư mục. Không du nhập thư viện/pattern mới nếu chưa được duyệt (xem mục 7).
- Mỗi thay đổi hành vi phải kèm test. Xem `docs/50-qa.md` để biết loại test nào.

**Trước khi kết thúc**
- `make lint && make test && make verify-contracts`
- Cập nhật doc miền **nếu và chỉ nếu** một ràng buộc đổi. Không thêm mô tả code vào doc — code tự mô tả nó.

---

## 6. Khi docs và code mâu thuẫn

Thứ tự ưu tiên: **contracts/ > docs/ > code**.

Nếu code làm khác doc:
1. Không tự sửa doc cho khớp code
2. Không tự sửa code cho khớp doc
3. **Báo cho người**, nêu rõ chỗ lệch, chờ quyết định

Lý do: doc ở đây ghi *ràng buộc chủ ý*, không phải mô tả. Code lệch doc thường là bug, không phải doc lỗi thời.

---

## 7. Cần người duyệt trước khi làm

Agent **không tự quyết** những việc sau:

- **Làm bất cứ việc gì thuộc stage sau** (xem mục 0b)
- Thêm dependency mới (thư viện, dịch vụ, hạ tầng)
- Đổi schema DB theo kiểu phá vỡ tương thích (xem `31-database.md` về expand/contract)
- Thêm block type mới vào SDUI
- Đổi cấu trúc auth hoặc phạm vi quyền
- Thêm platform channel mới (hiện giới hạn 4 — xem `11-merchant-app.md`)
- Bất cứ thứ gì đụng luồng tiền hoặc đối soát thanh toán
- **Bất cứ thứ gì liên quan tới Zalo** — hoãn vô thời hạn

---

## 8. Phong cách

- **Tiếng Việt** cho comment giải thích *tại sao*. Tên biến/hàm/class tiếng Anh.
- Comment giải thích **tại sao**, không giải thích **cái gì**. `// tăng giá không được ảnh hưởng đơn cũ` — tốt. `// gán giá vào biến` — xoá.
- Không thêm abstraction "cho tương lai". Viết cho yêu cầu hiện tại.
- Không sinh code chết, không để `TODO` mồ côi. TODO phải có tên người hoặc issue.
- Commit: `<scope>: <việc>` — ví dụ `backend: thêm idempotency cho tạo đơn`.

---

## 9. Điều quan trọng nhất

Repo này phục vụ chủ quán ăn ở tỉnh, dùng máy Android tầm thấp, mạng 3G chập chờn, lúc 7 giờ tối đông khách.

Khi phải chọn giữa **giải pháp thanh lịch** và **giải pháp không bao giờ sót đơn**, chọn cái thứ hai.
