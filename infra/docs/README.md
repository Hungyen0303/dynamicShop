# infra/docs

## Hiện tại
Chỉ Postgres qua Docker (`infra/docker/`). Người dùng đã có Docker sẵn.

```bash
cd infra/docker && cp .env.example .env && docker compose up -d
```

## Hai role — kiến trúc multi-tenant phụ thuộc vào chúng

| Role | Đặc điểm | Dùng ở |
|---|---|---|
| `app_user` | Chịu RLS | public plane + merchant plane |
| `app_admin` | `BYPASSRLS` | **chỉ** package `admin/` |

Tách vật lý để không thể vô tình dùng nhầm. Xem `docs/31-database.md`.

## Production (chưa dựng)
```
1 VPS (Singapore) + Docker Compose
  ├── Spring Boot
  ├── Postgres (+ backup hằng ngày ra object storage)
  └── Caddy (TLS tự động)
```

**Đừng dùng Kubernetes.** Ở quy mô 10–100 shop nó ăn nhiều tuần mà không đem lại gì.

🔴 **Diễn tập khôi phục backup một lần trước khi có shop thật.** Backup chưa từng restore thành công thì không phải backup.

## Cần hỏi người
- Đã có VPS chưa? Nhà cung cấp, vùng nào?
- Tên miền và subdomain (`api.`, `ops.`, `s.`)?
- Object storage cho backup?
