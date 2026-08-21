# tools/seed

Dựng storefront hàng loạt. **Thay thế studio cho 10 shop đầu tiên.**

```bash
./seed-storefront.sh \
  --tenant=quan-bun-co-ba \
  --template=fnb-basic \
  --logo=./logos/co-ba.png \
  --primary="#E23744"
```

## Đây là công cụ dùng thật, không phải script demo

Quy trình bán hàng: tối hôm trước chọn 20 quán trong cụm 500m, vào Facebook họ chụp menu, dựng sẵn 20 storefront. Sáng hôm sau đi vào đưa điện thoại: *"Em làm sẵn trang bán hàng cho quán mình rồi, anh xem thử 30 giây."*

Đó là lúc "3h" thành vũ khí thật — không hứa, đã làm xong rồi.

**Ưu tiên tốc độ, không cần đẹp.** Mục tiêu: 2–3 phút một quán.

## Template
- `fnb-basic` — quán ăn cơ bản
- `fnb-drinks` — quán nước, trà sữa
- (thêm khi cần)

Template là file JSON layout trong `templates/`, tuân theo `contracts/storefront.schema.json`.
