# customer_app/docs

| File | Nội dung |
|---|---|
| `../../../docs/10-customer-app.md` | **Ràng buộc chính** — đọc trước |
| `../../../docs/60-design.md` | Token, component |
| `flavors.md` | Cấu hình 4 flavor |
| `../../../docs/business/01-tech-customer-app.md` | Bối cảnh và lý do |

## Nhắc nhanh
- 🟢 **Stage 0** — cùng backend, là hai thứ duy nhất được làm bây giờ
- **Stage 0 chỉ 3 flavor môi trường.** Flavor theo shop (mô hình bán đứt) là Stage 2+.
- **Menu, giá, theme LUÔN tải runtime**, không nhồi vào binary
- Hai chốt chặn trong registry là bắt buộc.
- Ba tầng fallback: server → cache → bundled.
- `studio_web` là Stage 4 và có thể không bao giờ làm — đừng lo về nó bây giờ.
