---
description: Khởi tạo một phần của dự án (backend, customer_app, merchant_app, studio_web, admin, infra)
argument-hint: <backend|customer_app|merchant_app|studio_web|admin|infra|packages>
---

Khởi tạo phần: **$1**

## 🔴 Kiểm tra stage trước
Đọc `docs/70-stages.md`. Phần này có thuộc **stage hiện tại (Stage 0)** không?
Nếu không: **dừng lại**, nói rõ nó ở stage nào, hỏi người có muốn làm sớm không.

Stage 0 chỉ có: root, infra (Docker Postgres), contracts tối thiểu, backend, customer_app, mock data.

## Trước khi làm
1. Đọc `INIT.md` ở root — đặc biệt mục "Luật quan trọng nhất": thiếu gì thì **hỏi người, đừng đoán**
2. Đọc `INIT.md` trong thư mục của phần này (nếu có)
3. Đọc doc miền tương ứng trong `docs/`
4. Kiểm tra thứ tự init ở `INIT.md` mục 1 — phần này có phụ thuộc phần nào chưa init không?

## Trong lúc làm
- Gặp chỗ trống (tên miền, project ID, phiên bản công cụ, khoá bí mật) ⇒ **dừng, hỏi, chờ trả lời**
- Đề xuất một mặc định kèm câu hỏi để người dễ trả lời nhanh
- Không tự thêm dependency ngoài danh sách trong `INIT.md` mà chưa hỏi

## Sau khi làm
- Chạy phần checklist tương ứng ở `INIT.md` mục 13
- Báo cáo: đã làm gì, còn treo gì, cần người cung cấp gì
