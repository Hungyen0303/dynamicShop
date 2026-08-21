---
description: Chạy toàn bộ kiểm tra trước khi báo hoàn thành
---

## Stage 0

```bash
cd backend && ./gradlew test        # gồm test cô lập tenant
```

Cộng với kiểm tra tay **8 bước flow đầu-cuối** trong `docs/70-stages.md` — phần này chưa tự động hoá được ở Stage 0, phải chạy thật.

## Stage 1+ (chưa dựng)

```bash
make lint && make test && make verify-contracts
```

## Nếu có lệnh không chạy được
**Nói rõ ra.** Đừng bỏ qua im lặng rồi báo "đã xong".

## Drift guard — xác nhận từng cái
| Guard | Ý nghĩa khi fail |
|---|---|
| `verify:contracts` | Ai đó sửa file generated bằng tay |
| `verify:blocks` | Thêm block trong code mà quên contract |
| `verify:openapi` | Endpoint đổi mà quên contract |
| `verify:tokens` | Thêm token bằng tay |
| `verify:states` | Thêm trạng thái mà quên contract |
| `verify:metrics` | Web và BE tính chỉ số khác nhau |
| `lint:hardcode` | Hardcode style trong package dùng chung |
| `lint:money` | `double`/`float` trong domain BE |
| `lint:nativequery` | `nativeQuery` bỏ qua tenant filter |
| `verify:docs-links` | Doc trỏ file đã đổi tên |

**Không được** xoá hoặc `@Disabled` một guard để pass CI. Guard sai thì sửa guard và ghi lý do.

## Nếu thay đổi đụng merchant app luồng nhận đơn
Nhắc người rằng kịch bản máy thật (`docs/50-qa.md`) chưa tự động hoá được và cần chạy tay.
