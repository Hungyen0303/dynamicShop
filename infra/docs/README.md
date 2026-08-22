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

## Production — hình dạng đã dựng, chưa có VPS thật (Stage 1, 2026-08-22)

```
infra/docker/docker-compose.prod.yml   Postgres + Spring Boot (backend/Dockerfile) + Caddy
infra/docker/Caddyfile                 reverse proxy, TLS tự động (Let's Encrypt khi có domain thật)
infra/docker/.env.prod.example         copy sang .env.prod, điền giá trị thật trước khi lên VPS
```

**Đã test THẬT ở local** (2026-08-22) — build image, chạy `docker compose --env-file .env.prod
-f docker-compose.prod.yml up -d --build`, curl qua Caddy (`https://localhost:8443/actuator/health`
với `-k` vì Caddy dùng CA nội bộ cho `localhost`, không phải Let's Encrypt) → `200 {"status":"UP"}`,
xác nhận chuỗi container → backend → postgres chạy đúng trước khi có VPS thật.

🔴 **Bài học thật, đọc trước khi sửa `docker-compose.prod.yml`:** lần đầu dựng file, nó dùng
project name mặc định trùng với `docker-compose.yml` (cùng thư mục) và service key `postgres`
trùng tên → Docker Compose coi hai container là MỘT service, "recreate" mất container Postgres
dev đang chạy (volume `docker_ds-pgdata` may mắn không mất, phục hồi được bằng
`docker compose -f docker-compose.yml up -d`). Đã sửa bằng cách ghim `name: ds-prod` ở đầu file
— **đừng bao giờ xoá dòng `name:` đó**, và đừng đặt container/service trùng tên giữa hai file
compose trong cùng thư mục này nữa.

**Chưa dựng — cần chủ dự án cung cấp (xem `missing_config.md`):** VPS thật (provider/IP/SSH),
domain thật + DNS trỏ về VPS, backup Postgres hằng ngày ra object storage.

**Đừng dùng Kubernetes.** Ở quy mô 10–100 shop nó ăn nhiều tuần mà không đem lại gì.

🔴 **Diễn tập khôi phục backup một lần trước khi có shop thật.** Backup chưa từng restore thành công thì không phải backup. (Job backup tự động CHƯA làm — chỉ mới có hạ tầng chạy app.)

## Cần hỏi người
- Đã có VPS chưa? Nhà cung cấp, vùng nào?
- Tên miền và subdomain (`api.`, `ops.`, `s.`)?
- Object storage cho backup?
