# Flavor

| Flavor | Dùng cho | Đặc điểm |
|---|---|---|
| `dev` | Phát triển | Trỏ API local/dev, log verbose |
| `staging` | Test nội bộ | API staging, Firebase App Distribution |
| `prod` | Play Store / App Store | API production |
| `whitelabel` | Gói app riêng (3–5tr) | Khoá cứng `DEFAULT_TENANT_ID`, đổi icon/tên/bundle id |

## 🔴 Không tạo flavor cho từng shop
100 shop = 100 build = 100 lần submit store, và USP "lên app trong 3h" chết ngay.

Cấu hình shop tải **lúc runtime** theo `tenant_id`. Flavor chỉ phân biệt **môi trường**.

## whitelabel
Chỉ khác ở: khoá cứng một tenant, đổi app icon/tên/bundle id, ẩn phần chuyển shop.

Publish dưới **developer account của chính shop**, không phải account DynamicShop — đây là cách chuẩn để qua Apple Guideline 4.3.

## Cần hỏi người
- Application id / bundle id cho từng flavor
- Firebase project — dev và prod riêng hay chung
- Tên miền deep link
