# contracts/templates

Layout **khởi tạo** khi bán app cho một shop mới. Không phải khoá — là điểm xuất phát.

```
Lúc bán app:    chọn template → có ngay layout hợp lý
Sau đó:         chủ quán tự sắp xếp, chỉnh, ẩn/hiện qua merchant app
Bất cứ lúc nào: nút "Đặt lại về template" nếu nghịch hỏng
```

Không có template thì shop mới bắt đầu từ trang trắng — chủ quán không biết bắt đầu từ đâu.

| Template | Phù hợp |
|---|---|
| `fnb-do-an.json` | Quán ăn: bún, phở, cơm |
| `fnb-do-uong.json` | Trà sữa, cà phê |

Template tuân theo `../storefront.schema.json`.
