# Auth admin

## Ba hệ tách rời — không trộn

| Hệ | Đăng nhập | Phạm vi |
|---|---|---|
| customer | Zalo / OTP | không quyền quản trị |
| merchant | SĐT + mật khẩu, gắn `tenant_id` | chỉ tenant của mình |
| **admin** | tài khoản riêng + **TOTP 2FA** | **mọi** tenant |

## Bốn việc bắt buộc

1. **Subdomain riêng** — `ops.dynamicshop.vn`, JWT `audience` khác
2. **Token merchant không bao giờ escalate được thành admin** — lỗi phổ biến khi dùng chung bảng `users`. Tách bảng, tách issuer. **Viết test cho điều này.**
3. **Giới hạn IP hoặc VPN** ở giai đoạn đầu — rẻ, tránh được rủi ro lớn nhất
4. **DataSource `app_admin`** (BYPASSRLS) chỉ dùng trong package `admin/` của backend

## Impersonate
Bắt buộc: audit log (ai xem tenant nào, lúc nào) + banner đỏ rõ ràng trên UI khi đang ở chế độ này.

Bạn đang nắm dữ liệu khách hàng của người khác. Một ngày nào đó sẽ có người hỏi.
