-- DynamicShop — khởi tạo role cho multi-tenant
-- Chạy tự động một lần khi volume postgres còn trống.
--
-- KIẾN TRÚC (xem docs/31-database.md):
--   app_user   — role thường, CHỊU Row Level Security.
--                Dùng cho public plane + merchant plane.
--   app_admin  — role BYPASSRLS, chỉ dùng trong package admin/.
--                Tách vật lý để không thể vô tình dùng nhầm.
--
-- KHÔNG đổi cấu trúc hai role này.

\set app_user_password `echo "$APP_USER_PASSWORD"`
\set app_admin_password `echo "$APP_ADMIN_PASSWORD"`

CREATE ROLE app_user  LOGIN PASSWORD :'app_user_password';
CREATE ROLE app_admin LOGIN PASSWORD :'app_admin_password' BYPASSRLS;

GRANT CONNECT ON DATABASE dynamicshop TO app_user, app_admin;
GRANT USAGE  ON SCHEMA public         TO app_user, app_admin;

-- Flyway chạy bằng superuser sẽ tạo bảng; cấp quyền mặc định cho hai role
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user, app_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO app_user, app_admin;

-- GUC dùng cho RLS policy. Backend phát `SET LOCAL app.tenant_id = '...'`
-- ở đầu mỗi transaction. LƯU Ý: SET LOCAL, không phải SET.
-- Dùng SET sẽ rò rỉ tenant qua connection pool — xem docs/31-database.md.
