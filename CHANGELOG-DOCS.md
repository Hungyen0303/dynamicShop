# Thay đổi bộ docs

## 2026-08 — Điều chỉnh lớn

### Thêm mới
- **`docs/70-stages.md`** — chia 6 giai đoạn, local-first. **Agent phải đọc trước mọi task.**
- **`docs/80-git-workflow.md`** — trả lời "sửa mobile thì BE có thấy không": monorepo + `git sparse-checkout`.

### Đổi chiến lược
| Trước | Sau |
|---|---|
| Dynamic App (app tổng chứa sub-app), USP "lên app trong 3h" | **Bán đứt app riêng cho từng shop**, flavor theo shop được chấp nhận |
| Thuê bao theo tháng | **Trả một lần + hạn mức dung lượng**; bảo trì & cập nhật tính phí riêng |
| Zalo Login là phương án chính | **Zalo hoãn vô thời hạn** |
| studio_web là công cụ vận hành chính | **Stage 4, có thể không bao giờ làm** — chủ quán đăng nội dung từ điện thoại |
| Next.js admin ở Stage 5–6 | **Stage 3, tối giản** |
| merchant_app song song backend | **Stage 2**, sau khi Firebase ổn |

### Hoãn xuống stage sau
in bill nhiệt (5) · CI/CD (3) · seed script (4) · melos (4) · Firebase (1) · deploy (1) · đối soát tự động (5)

### Ràng buộc mới
- **Menu/giá/theme/layout LUÔN tải runtime.** Flavor chỉ chứa danh tính (tên app, icon, tenant mặc định). Nếu nhồi nội dung vào binary thì đổi giá một món phải submit store lại.
- **Quota dung lượng theo tenant** + **export dữ liệu** — cột đã có trong schema từ V1, tính năng làm ở Stage 3.
- **Mock data 2 shop** (V900, V901) phải khác nhau rõ rệt về theme và layout.

## 2026-08 (bổ sung) — Merchant app là studio

Làm rõ phạm vi cấu hình của chủ quán. Trước đó bị thu hẹp nhầm thành "chỉ ẩn/hiện".

### Đúng phạm vi
Chủ quán cấu hình **đầy đủ trên điện thoại**: đổi thứ tự block (kéo thả), ẩn/hiện, chọn style block, chỉnh `borderRadius` và `background` **từng block**, đổi theme và font.

Component không có màn hình chỉnh riêng nhưng **đổi gián tiếp qua block chứa nó** — chuỗi `blockOverride ?? variantPreset ?? tenantTheme ?? appDefault` giữ nguyên, chủ quán điều khiển ba tầng đầu.

### Thay đổi kiến trúc
| Trước | Sau |
|---|---|
| merchant_app **không** import `ds_sdui` | merchant_app **cần** `ds_blocks` + `ds_sdui` — xem trước trực tiếp phải dùng đúng `renderStorefront()` |
| studio_web Stage 4, có thể không làm | studio_web **gần như chắc chắn không làm** — merchant app đã là studio |
| `overridable` là mảng tên thuộc tính | `overridable` là **object kèm metadata control** (`slider`/`color_token`/`segmented`/`toggle` + min/max/label) để merchant app **sinh UI tự động** |

### Thêm mới
- `contracts/templates/` — layout khởi tạo lúc bán app (`fnb-do-an`, `fnb-do-uong`). Không khoá, chỉ là điểm xuất phát; có nút "Đặt lại về template".
