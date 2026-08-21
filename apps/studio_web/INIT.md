# INIT — studio_web

> 🔴 **STAGE 4 — CHƯA LÀM, VÀ CÓ THỂ KHÔNG BAO GIỜ LÀM.**
>
> Chủ quán không ngồi laptop. Họ chụp ảnh món ăn hoặc quay video bằng điện thoại
> và đăng trực tiếp từ đó — nên quản lý nội dung nằm ở `merchant_app`, không ở web.
>
> Chỉ làm nếu thực tế chứng minh là cần. **Xác nhận với người trước.**

---

Đọc `../../INIT.md` mục 9 trước.

## Phụ thuộc
Packages Flutter phải init xong. `web/admin` nên init trước để biết đường dẫn nhúng.

## Việc
```bash
flutter create --platforms=web studio_web
```
- Build CanvasKit
- Import `ds_sdui` + `ds_blocks` + `ds_tokens`
- Bridge `postMessage` có kiểm tra `origin`
- Script build ra `web/admin/public/studio/`

## Xong khi
- [ ] Build được cho web
- [ ] Preview khớp golden của `customer_app` cùng layout
- [ ] Bundle lazy-load, không tải ở trang chủ admin
