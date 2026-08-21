# INIT — customer_app

🟢 **Stage 0** — làm sau khi backend chạy được.

Đọc `../../INIT.md` mục 5 và `../../docs/70-stages.md` trước. Luật: **thiếu gì thì hỏi người, đừng đoán.**

## Phụ thuộc
Backend Stage 0 phải chạy được trước.

## Cần hỏi người
1. Phiên bản Flutter? Dùng FVM không?
2. Test trên emulator hay máy Android thật?

**Chưa cần ở Stage 0:** Firebase, bundle id thật, deep link, Zalo, tài khoản store.

## Việc
```bash
flutter create --org vn.dynamicshop --platforms=android customer_app
```
- **Chỉ 3 flavor môi trường:** dev / staging / prod. Flavor theo shop là Stage 2+.
- **Chưa dùng melos** — `path:` dependency trực tiếp trong `pubspec.yaml`
- Trỏ về `http://10.0.2.2:8080` (emulator nhìn thấy localhost máy host)
- **Màn hình dev đổi giữa mock shop A và B** — cần cho bar hoàn thành
- `assets/default_storefront.json` — fallback tầng 3

## Xong khi (Stage 0)
- [ ] Build và chạy được trên emulator
- [ ] Render đúng storefront của mock shop A
- [ ] Đổi sang shop B → **theme và menu khác hẳn**
- [ ] Đặt được đơn, đơn vào DB
- [ ] Chặn API → vẫn render được từ bundled
- [ ] 2–3 golden test tiêu biểu (chưa cần đủ 8 block)
