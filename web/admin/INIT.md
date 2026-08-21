# INIT — web/admin

> 🔴 **STAGE 3 — CHƯA LÀM.**
>
> Và khi làm thì **tối giản**: đây là công cụ của riêng bạn (operator), không phải sản phẩm cho chủ shop.
> Phạm vi: quản lý tenant, quota dung lượng, 6 chỉ số, export dữ liệu.
>
> Xem `../../docs/70-stages.md`. **Xác nhận với người trước khi bắt đầu.**

---

Đọc `../../INIT.md` mục 10 trước. Luật: **thiếu gì thì hỏi người, đừng đoán.**

## Cần hỏi người
1. pnpm / npm / yarn? (đề xuất pnpm)
2. Deploy Vercel hay cùng VPS với backend?
3. Subdomain cho admin?

## Việc
```bash
cd web && pnpm create next-app@latest admin --typescript --app --tailwind
```
- Auth admin tách hoàn toàn khỏi merchant (subdomain, JWT audience riêng)
- TOTP 2FA bắt buộc
- Trang chủ = 6 chỉ số, công thức từ `contracts/metrics.json` (**không tính lại ở web**)
- `/studio/[id]` nhúng Flutter web, lazy-load
- Audit log cho impersonate

## Xong khi
- [ ] `pnpm dev` chạy được
- [ ] 2FA hoạt động, không tắt được
- [ ] Có test: token merchant **không** truy cập được endpoint admin
- [ ] Trang 6 chỉ số hiển thị đúng, không tự tính
- [ ] `postMessage` kiểm tra `origin`
