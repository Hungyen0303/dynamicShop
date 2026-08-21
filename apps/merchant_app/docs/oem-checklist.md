# OEM giết app nền

Nguyên nhân hàng đầu khiến app bán hàng ở VN sót thông báo. **Không xuất hiện trên Pixel hay emulator.**

Xiaomi (MIUI), Oppo (ColorOS), Vivo (FuntouchOS), Realme, Samsung đều kill foreground service, chặn FCM khi app bị "đóng băng", và không cho autostart sau khi khởi động máy.

## Bốn thứ bắt buộc

1. **Xin miễn tối ưu pin** — `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, hiện dialog **ngay trong onboarding**, không để sau
2. **Hướng dẫn autostart theo hãng** — detect `Build.MANUFACTURER`, mở thẳng màn hình cài đặt tương ứng kèm ảnh minh hoạ từng bước
3. **Foreground service** với notification thường trực ("Đang nhận đơn")
4. **Màn hình tự kiểm tra** — "Trạng thái nhận đơn: ✅ Bình thường / ⚠️ Có thể bị chặn"

Mục 2 và 4 là **tính năng chính**, không phải việc phụ. Chúng tách app dùng được khỏi app bị bỏ.

## Intent cài đặt theo hãng
Cần tra và cập nhật theo phiên bản OS — danh sách này thay đổi. Nếu không chắc intent nào đúng cho một hãng, **hỏi người** hoặc để fallback mở màn hình cài đặt chung.

## Test
Kịch bản 1, 2, 7 trong `docs/50-qa.md` **phải chạy trên máy Xiaomi/Oppo thật**. Emulator không có lớp quản lý pin của OEM — đó chính là thứ cần test.

Ghi kết quả vào `docs/qa/device-matrix.md` kèm model máy và phiên bản OS.
