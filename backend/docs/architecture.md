# Kiến trúc backend

## Monolith, tách module bằng package

```
vn/dynamicshop/
  common/        TenantContext, security, error, idempotency, outbox helper
  storefront/    public plane — đọc storefront, resolve data_ref
  catalog/       product, category
  order/         state machine, order_events
  payment/       payment_events, đối soát
  merchant/      API cho merchant app (sync, xác nhận đơn, báo cáo)
  admin/         API cho operator — DataSource BYPASSRLS, luôn audit log
  outbox/        bảng outbox + worker
  notification/  FCM adapter
```

**Package không gọi chéo repository của nhau.** Giao tiếp qua service công khai.

## Luồng request

```
Request
  → TenantFilter        (resolve tenant từ JWT claim hoặc slug)
  → SecurityFilter      (xác thực, phân quyền theo mặt phẳng)
  → @Transactional      (SET LOCAL app.tenant_id ngay đầu transaction)
  → Service             (business logic)
  → Repository          (Hibernate @TenantId + RLS)
```

## Ba mặt phẳng route

| Prefix | Tenant từ | Auth | DataSource |
|---|---|---|---|
| `/v1/s/{slug}/…` | slug | không | `app_user` |
| `/v1/merchant/…` | JWT claim | có | `app_user` |
| `/v1/admin/…` | không giới hạn | có + 2FA | `app_admin` (BYPASSRLS) |

## Thứ tự triển khai
```
1. application.yml + kết nối Docker Postgres
2. Flyway V1__init.sql: bảng lõi + RLS + policy
3. TenantContext + filter + SET LOCAL
4. TEST CÔ LẬP TENANT          ← phải xanh trước khi làm tiếp
5. Auth (2 mặt phẳng)
6. Catalog + storefront API
7. Order + state machine + order_events
8. Idempotency
9. Outbox + FCM
```
🔴 Không viết endpoint nào trước khi bước 4 xanh.
