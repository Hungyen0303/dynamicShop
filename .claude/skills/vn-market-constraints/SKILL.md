---
name: vn-market-constraints
description: Dùng khi làm bất cứ tính năng nào hướng tới người dùng cuối — UI, thông báo, thanh toán, in ấn, xử lý mạng, font, hoặc báo cáo. Chứa các ràng buộc thực tế của thị trường Việt Nam ở tỉnh mà nếu không biết sẽ viết code sai một cách hợp lý.
---

# Ràng buộc thị trường Việt Nam

Người dùng: chủ quán ăn nhỏ ở tỉnh và khách của họ. Những ràng buộc dưới đây **không suy ra được từ code** — chúng đến từ thực địa.

## Thiết bị & mạng
- Máy Android tầm thấp, 2–3GB RAM, thường Xiaomi / Oppo / Vivo / Realme
- 3G chập chờn, mất kết nối 30 giây là bình thường
- Mọi tính năng phải dùng được khi mạng chậm hoặc mất

## 🔴 OEM giết app nền — vấn đề số 1
MIUI, ColorOS, FuntouchOS kill foreground service, chặn FCM, không cho autostart. **Không xuất hiện trên Pixel hay emulator.**

Bắt buộc: xin miễn tối ưu pin, hướng dẫn autostart theo `Build.MANUFACTURER`, foreground service, và màn hình tự kiểm tra trạng thái nhận đơn.

## Chuông báo đơn
Quán 7 giờ tối rất ồn. Chuông mặc định sẽ bị bỏ lỡ.
- Lặp cho tới khi có người bấm xác nhận
- Stream **ALARM**, không phải NOTIFICATION (ALARM không bị chế độ im lặng tắt)
- Full-screen intent + rung

## Font tiếng Việt
Nhiều font đẹp **không đủ dấu**. Chữ hiện ra kiểu "Trà s?a" hoặc dấu lệch — chỉ phát hiện khi shop gửi ảnh chụp màn hình.
- Chỉ dùng font trong `allowed_fonts` của `contracts/tokens.json`
- Chuỗi test: `Trà sữa trân châu đường đen — Phở bò tái nạm gầu`

## In bill nhiệt
> ⚠️ Stage 5 — hoãn. Phần dưới để tham khảo khi tới lúc.

Máy in Trung Quốc thường **không có codepage tiếng Việt**. Cách bền nhất: **render bill thành bitmap rồi in ảnh**. Chậm hơn ~200ms nhưng luôn đúng dấu. Đây là mặc định, không phải phương án dự phòng.

Khổ giấy cấu hình được: K80 ≈ 48 ký tự/dòng, K58 ≈ 32.

## Thanh toán
- **Tiền đi thẳng vào tài khoản shop.** Backend không bao giờ giữ tiền — vừa là lợi thế bán hàng vừa là ranh giới pháp lý.
- VietQR + mã tham chiếu **ngắn** (6–8 ký tự) — nhiều ngân hàng cắt nội dung dài
- MVP là nút "Đã nhận tiền" thủ công, không phải webhook tự động

## Đăng nhập
🔴 **Zalo đang hoãn vô thời hạn theo quyết định của chủ dự án.** Không tự ý thêm Zalo Login / OA / ZNS.

Stage 0: JWT + mật khẩu trong fixture. Chọn phương án đăng nhập thật ở Stage 1 — **hỏi người**.

## Ngày kinh doanh ≠ ngày lịch
Quán ăn đêm đóng cửa 2h sáng. Đơn lúc 1h sáng, với chủ quán, là doanh thu của **tối hôm qua**. Cắt theo nửa đêm ⇒ báo cáo luôn sai ⇒ chủ quán không tin số liệu.

Dùng `tenants.business_day_start` (mặc định `04:00`).

## Báo cáo trên điện thoại
Không bê biểu đồ web xuống. Một con số lớn + một so sánh. Ba dòng, không phải ba biểu đồ.

Push tổng kết cuối ngày là tính năng ROI cao nhất: chủ quán không cần mở app vẫn thấy giá trị mỗi ngày.

## Chủ quán
Không rành công nghệ, không đọc hướng dẫn, đang bận, tay dính dầu mỡ. Vùng chạm ≥48dp, chữ ≥16sp, tương phản ≥4.5:1.
