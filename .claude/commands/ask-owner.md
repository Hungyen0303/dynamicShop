---
description: Gom các câu hỏi còn treo thành một danh sách để hỏi chủ dự án một lượt
---

Rà soát công việc hiện tại và gom **mọi chỗ đang thiếu thông tin** thành một danh sách.

## Nguyên tắc
Chủ dự án bận. Hỏi gộp một lượt tốt hơn hỏi lắt nhắt nhiều lần.

## Với mỗi câu hỏi, cung cấp
1. **Cần gì** — cụ thể, một dòng
2. **Vì sao cần** — chặn việc gì
3. **Mặc định em đề xuất** — để người chỉ cần trả lời "ok" hoặc sửa

## Định dạng
```
1. [CHẶN] Firebase service account JSON
   Vì: không gửi được FCM, chặn sprint 4
   Đề xuất: tạo project `dynamicshop-dev`, anh cho em file JSON

2. [KHÔNG CHẶN] Tên miền deep link
   Vì: cần để cấu hình assetlinks.json
   Đề xuất: `s.dynamicshop.vn`
```

Đánh dấu `[CHẶN]` cho thứ đang chặn việc, `[KHÔNG CHẶN]` cho thứ có thể làm sau. Tham chiếu `INIT.md` mục "Danh sách thông tin cần hỏi" để không sót.
