# 00 — Context chung

**Mọi agent đọc file này trước, bất kể task gì.** Ngắn có chủ ý.

Đọc kèm bắt buộc: **`70-stages.md`** — nó quyết định việc gì được phép làm bây giờ.

---

## Sản phẩm trong một đoạn

DynamicShop bán **app bán hàng riêng cho từng quán ăn nhỏ ở tỉnh**. Shop tự có khách (Facebook, Zalo, khách quen) nhưng đang mất 20–30% cho sàn hoặc quản lý bằng tay. DynamicShop cho họ app mang thương hiệu của chính họ, nhận đơn, và tiền về **thẳng tài khoản shop**.

**Không phải sàn.** Không xếp hạng quán, không bán vị trí hiển thị, không điều phối đơn.

## Mô hình kinh doanh (cập nhật 08/2026)

**Bán đứt app** — trả một lần, không thuê bao:

| Khoản | Cách tính |
|---|---|
| App riêng cho shop | Trả một lần |
| **Dung lượng lưu trữ** | Có hạn mức theo shop. Đầy → shop tự **export dữ liệu ra Drive/Dropbox** rồi dọn |
| Bảo trì & cập nhật | **Tính phí riêng** |

Hệ quả kỹ thuật:
- Phải theo dõi **quota dung lượng** theo tenant (ảnh chiếm phần lớn)
- Phải có tính năng **export dữ liệu khách hàng** cho shop tự sao lưu
- Mỗi shop có thể có **flavor riêng** (xem `10-customer-app.md`)

---

## Thuật ngữ — dùng đúng từ này trong code và commit

| Từ | Nghĩa chính xác |
|---|---|
| **tenant** | Một shop. Đơn vị cô lập dữ liệu. `tenant_id` là UUID. |
| **merchant** | Con người — chủ shop hoặc nhân viên. Thuộc về đúng một tenant. |
| **customer** | Khách mua hàng. **Toàn cục, không thuộc tenant nào.** |
| **operator** | Nhân sự DynamicShop, dùng admin web. Thấy mọi tenant. |
| **storefront** | Trang bán hàng của một tenant |
| **block** | Một khối UI trong storefront (`hero_banner`, `product_grid`…) |
| **SDUI** | Server-Driven UI — layout do server quyết, mức 2 (danh sách block phẳng) |
| **token** | Biến design (màu, radius, spacing). Nguồn: `contracts/tokens.json` |
| **flavor** | Bản build riêng: môi trường (`dev`/`staging`/`prod`) hoặc một shop cụ thể |
| **quota** | Hạn mức dung lượng của một tenant |

⚠️ **Không dùng lẫn "shop" và "tenant" trong code.** Chọn `tenant` cho mọi thứ kỹ thuật. `shop` chỉ dùng trong text hiển thị.

---

## Hai mặt phẳng bảo mật

Đặc điểm quan trọng nhất của hệ thống, khác với multi-tenant SaaS thông thường.

| | Public plane | Authenticated plane |
|---|---|---|
| Ai | customer | merchant, operator |
| Route | `/v1/s/{slug}/…` | `/v1/merchant/…`, `/v1/admin/…` |
| Tenant lấy từ | **slug trong path** | **JWT claim** |
| Được làm gì | đọc dữ liệu public, tạo đơn cho chính mình | toàn quyền trong tenant của mình |
| Identity | `user_id` toàn cục | `user_id` + `tenant_id` |

Customer **không đăng nhập vào tenant nào**. Đơn hàng thuộc cặp `(tenant_id, user_id)`.

---

## Các bề mặt và mức ưu tiên

```
backend       Spring Boot          Stage 0  ← ưu tiên cao nhất
customer_app  Flutter              Stage 0
merchant_app  Flutter, Android     Stage 2
web/admin     Next.js, TỐI GIẢN    Stage 3
studio_web    Flutter web          Stage 4, có thể KHÔNG BAO GIỜ
```

**Vì sao admin và studio bị hạ ưu tiên:** chủ quán không ngồi laptop. Họ chụp ảnh món ăn hoặc quay video **bằng điện thoại** và đăng trực tiếp từ đó. Toàn bộ việc quản lý nội dung nằm trong **merchant_app**, không phải web.

---

## Ràng buộc thực tế của người dùng

- Máy Android tầm thấp, 2–3GB RAM, thường Xiaomi / Oppo / Vivo
- Mạng 3G chập chờn, mất kết nối 30 giây là bình thường
- Quán ăn lúc 7 giờ tối rất ồn
- Chủ quán không rành công nghệ, không đọc hướng dẫn, **làm mọi thứ trên điện thoại**
- **Sót một đơn = mất khách hàng đó vĩnh viễn**

Khi phải chọn giữa thanh lịch và tin cậy, chọn tin cậy.

---

## Tiền — luật tuyệt đối

1. Lưu `BIGINT` / `long`, đơn vị **đồng**. VND không có phần thập phân.
2. `order_items` snapshot `name_snapshot` + `unit_price` lúc đặt.
3. `payment_status` (`UNPAID | PAID | PARTIAL | REFUNDED`) tách rời `order_status`.
4. Tiền đi **thẳng từ khách sang tài khoản shop**. Backend không bao giờ giữ tiền.

---

## Đang hoãn vô thời hạn

**Zalo — mọi thứ liên quan.** Login, OA, ZNS, Mini App. Không tự ý thêm; hỏi người trước.

---

## Nguồn sự thật

```
contracts/  >  docs/  >  code
```

Nếu code mâu thuẫn với doc: **báo người, đừng tự sửa bên nào.**
