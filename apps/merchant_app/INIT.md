# INIT — merchant_app

> 🟢 **STAGE 2 — ĐANG LÀM, phạm vi bị cắt.** (cập nhật 2026-08-22, sprint 2.1b)
>
> Câu gốc "chưa làm, bắt đầu sau khi Stage 1 xong, dừng lại và xác nhận với người trước"
> **đã được chủ dự án override**: đi tiếp Stage 2 trong khi Stage 1 để treo chờ
> VPS/domain/Firebase/R2 thật.
>
> **Đọc `../../progress.md` mục 9 trước khi làm bất cứ gì** — ở đó có phạm vi sprint đã cắt,
> ranh giới cái gì làm được không cần credential, và **hai luật kiến trúc bắt buộc**
> (`IncomingOrderSink` duy nhất, `PushTokenProvider` là interface).
> API để gọi: `../../docs/90-api-contract.md` — **đọc file đó, không đọc `.java` của backend.**
>
> Không cần hỏi lại có được làm không. Nhưng đừng tự mở rộng ra ngoài sprint đã cắt.

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

⚠️ Bar này là của **cả Stage 2**, không phải của sprint 2.2. Hai mục đã đổi so với bản gốc:
in bill nhiệt thuộc **Stage 5** (`../../docs/70-stages.md`), không phải điều kiện đóng Stage 2;
và "chuông vẫn kêu" hiện chạy được bằng **polling-only** vì chưa có FCM thật (sprint 2.5).

- [ ] Build được APK debug
- [ ] **Chạy nền 8 tiếng trên máy Xiaomi/Oppo thật, chuông vẫn kêu** ← chạy sớm bằng polling-only,
      đừng đợi có FCM; bài test này tốn *thời gian đồng hồ* chứ không tốn công
- [ ] Khởi động lại máy → tự chạy lại
- [ ] Tắt mạng giữa lúc xác nhận đơn → vào queue, tự gửi lại
- [ ] Nhận cùng một đơn 2 lần (poll lặp hoặc push trùng) → **một** bản ghi đơn, chuông kêu **một** lần
- [ ] Token hết hạn giữa lúc app đang chạy nền → tự đăng nhập lại, **không sót đơn nào**
      (ép bằng `app.jwt.expiration-minutes: 2` ở local — TTL thật là 30 ngày nên đường này
      sẽ không bao giờ tự lộ ra khi test tay)
- [ ] ~~In bill đúng dấu tiếng Việt trên máy in thật~~ → **Stage 5**, không phải điều kiện Stage 2
