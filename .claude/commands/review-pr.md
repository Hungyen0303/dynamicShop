---
description: Rà soát thay đổi trước khi merge
---

Rà soát các thay đổi hiện tại.

## Trình tự
1. `git diff` để xem phạm vi thay đổi
2. Xác định thay đổi đụng những gì: tenant / auth / tiền / trạng thái đơn / API public / block / token
3. Chạy subagent `security-reviewer` nếu đụng bất kỳ mục nào ở trên
4. Chạy subagent `qa` để xác định test bắt buộc
5. Chạy `/verify`

## Câu hỏi bắt buộc trả lời
- Thay đổi này có nằm trong sprint hiện tại không? (nếu không rõ → subagent `pm`)
- Có sửa contract mà quên `make generate` không?
- Có sửa file `generated/` bằng tay không?
- Có sửa block mà chưa kiểm tra ảnh hưởng lên **cả** `customer_app` và `studio_web` không?
- Có `TODO` mồ côi không (không tên người, không issue)?
- Có bí mật nào bị commit không?

## Định dạng
```
PHẠM VI: (thay đổi đụng gì)
CHẶN MERGE: (nếu có)
CẦN SỬA:
ĐÃ KIỂM TRA: lint / test / verify-contracts
CÒN TREO: (việc cần người làm tay, ví dụ kịch bản máy thật)
```
