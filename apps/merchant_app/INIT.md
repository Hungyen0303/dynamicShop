# INIT — merchant_app

> 🔴 **STAGE 2 — CHƯA LÀM.**
>
> Bắt đầu sau khi Stage 1 xong (VPS + Firebase + FCM chạy ổn định).
> Xem `../../docs/70-stages.md`.
>
> **Nếu được giao task dựng app này, dừng lại và xác nhận với người trước.**

---

Đọc `../../INIT.md` mục 8 trước. Luật: **thiếu gì thì hỏi người, đừng đoán.**

## Cần hỏi người
1. Model máy in nhiệt để test (K80/K58, hãng gì)?
2. Có máy Xiaomi/Oppo thật để chạy kịch bản 8 tiếng không?
3. Firebase project + `google-services.json`?

## Việc
```bash
flutter create --org vn.dynamicshop --platforms=android merchant_app
```
**Chỉ Android.** iOS không đảm bảo chuông báo đơn khi background.

- Foreground service + channel `new_order` (importance MAX, stream ALARM)
- Xin `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- Hướng dẫn autostart theo `Build.MANUFACTURER`
- `drift` cho offline queue
- Platform channel: foreground service, máy in, battery/autostart (**tối đa 4**)

## Xong khi
- [ ] Build được APK debug
- [ ] **Chạy nền 8 tiếng trên máy thật, chuông vẫn kêu**
- [ ] Khởi động lại máy → tự chạy lại
- [ ] Tắt mạng giữa lúc xác nhận đơn → vào queue, tự gửi lại
- [ ] In bill đúng dấu tiếng Việt trên máy in thật
- [ ] Push trùng 2 lần → một bản ghi đơn
