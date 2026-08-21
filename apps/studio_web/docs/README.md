# studio_web/docs

| File | Nội dung |
|---|---|
| `../../../docs/20-web.md` | **Ràng buộc chính** — đọc trước |
| `../../../docs/10-customer-app.md` | Block, SDUI — dùng chung package |
| `bridge.md` | Giao tiếp postMessage với Next.js |

> 🔴 **Stage 4 — chưa làm, và có thể không bao giờ làm.**
> Chủ quán đăng nội dung từ điện thoại qua merchant_app, không qua web.

## Nhắc nhanh
- Đây là **công cụ nội bộ** cho operator, không phải sản phẩm cho chủ shop
- Tính năng quan trọng nhất là **nhân bản storefront**, không phải drag-drop
- **Không giữ state** — Next.js giữ hết, Flutter chỉ render
- Dùng chung `ds_blocks` với `customer_app` — đó là lý do preview không lệch
