# In bill nhiệt

**Thử package sẵn trước khi viết platform channel.** Mỗi channel là món nợ phải trả trên hai OS.

| Thư viện | Vai trò |
|---|---|
| `esc_pos_utils` | Sinh byte lệnh ESC/POS |
| `flutter_pos_printer_platform` / `blue_thermal_printer` | Truyền Bluetooth / USB |

## 🔴 Tiếng Việt có dấu
Máy in nhiệt Trung Quốc thường **không có codepage tiếng Việt**.

**Giải pháp mặc định: render bill thành bitmap rồi in ảnh.** Chậm hơn ~200ms nhưng luôn đúng dấu. Đây không phải phương án dự phòng — đây là cách làm chính.

## Khổ giấy
K80 = 80mm ≈ 48 ký tự/dòng. K58 = 58mm ≈ 32 ký tự. **Cho cấu hình, đừng hardcode.**

## Bắt buộc có
- Tự động reconnect — máy in mất kết nối là chuyện thường ngày
- Nút "In lại" trên mọi đơn
- Hàng đợi in khi máy in bận, không bỏ lệnh

## Test
Phải in thử trên **máy in thật**, kiểm tra chuỗi:
`Trà sữa trân châu đường đen — Phở bò tái nạm gầu`

## Cần hỏi người
Model máy in đang dùng để thử (hãng gì, K80 hay K58).
