# contracts/ — Nguồn sự thật

**Code không bao giờ là nguồn sự thật cho contract. Thư mục này mới là.**

| File | Định nghĩa | Sinh ra |
|---|---|---|
| `openapi.yaml` | Mọi HTTP endpoint | Client Dart (`ds_api`), type TS, interface Java |
| `storefront.schema.json` | Cấu trúc JSON storefront | Validator ở BE + FE, Zod schema |
| `blocks.registry.json` | Block, props, whitelist override | Registry Dart + form studio |
| `tokens.json` | Design token | `ds_tokens` (Dart) + `tokens.ts` |
| `order-states.json` | State machine đơn hàng | Enum + bảng transition |
| `metrics.json` | Công thức 6 chỉ số | Query BE; web **không** tính lại |
| `studio-bridge.schema.json` | Message giữa Next.js và Flutter studio | Type TS + model Dart |

## Luật

1. Sửa hành vi ⇒ sửa contract **trước**, `make generate`, rồi mới sửa code.
2. **Không bao giờ** sửa file đã sinh ra bằng tay — chúng có header `// GENERATED — DO NOT EDIT`.
3. CI chạy `make verify-contracts`. Code lệch contract ⇒ build fail.
4. Mọi field mới **phải có default**. Config cũ trong DB không có field đó và vẫn phải parse được.

Đây là cơ chế duy nhất giữ cho docs không nói dối sau 6 tháng và sau khi đổi agent.

## Stage 0 chỉ cần tối thiểu

Ở Stage 0, **viết model bằng tay nhanh hơn dựng generator.** Chỉ cần `storefront.schema.json` để thống nhất cấu trúc JSON giữa backend và app.

`openapi.yaml`, `make generate`, `make verify-contracts` là **Stage 1+**, khi API đã ổn định. Đừng dựng generator cho một API còn đang đổi mỗi ngày.

Nhưng **luật "mọi field phải có default" áp dụng ngay từ Stage 0** — nó rẻ bây giờ và rất đắt để sửa sau.

---

## Trạng thái

| File | Trạng thái |
|---|---|
| `tokens.json` | ✅ có bản khởi tạo |
| `blocks.registry.json` | ✅ có bản khởi tạo |
| `order-states.json` | ✅ có bản khởi tạo |
| `metrics.json` | ✅ có bản khởi tạo |
| `storefront.schema.json` | ⬜ cần dựng — xem `docs/10-customer-app.md` |
| `studio-bridge.schema.json` | ⬜ cần dựng — xem `docs/20-web.md` |
| `openapi.yaml` | ⬜ cần dựng — xem `docs/30-backend.md` |

Công cụ sinh code chưa chọn. **Hỏi người** trước khi quyết (`openapi-generator` / `orval` / script riêng).
