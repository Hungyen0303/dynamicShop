-- Mock tenant B — quán trà sữa, theme xanh (khác hẳn đỏ của tenant A). Chỉ nạp ở profile
-- local. UUID cố định để storefronts.layout tham chiếu đúng category_id.

INSERT INTO tenants (id, slug, name, status, timezone, business_day_start)
VALUES ('22222222-2222-2222-2222-222222222222', 'tra-sua-ngoc', 'Trà Sữa Ngọc', 'ACTIVE', 'Asia/Ho_Chi_Minh', '04:00');

-- Mật khẩu fixture: trasua123
INSERT INTO merchants (id, tenant_id, phone, password_hash, name, role)
VALUES ('22222222-2222-2222-2222-900000000001', '22222222-2222-2222-2222-222222222222',
        '0902000001', '$2a$10$qRo07CI8bJEmSPj9wJdZ.uayOvTLBoJ.fIoc4ao5uuiK3mlh410j.', 'Chị Ngọc', 'OWNER');

INSERT INTO categories (id, tenant_id, name, sort_order) VALUES
  ('22222222-2222-2222-2222-200000000001', '22222222-2222-2222-2222-222222222222', 'Trà sữa', 1),
  ('22222222-2222-2222-2222-200000000002', '22222222-2222-2222-2222-222222222222', 'Trà trái cây', 2);

INSERT INTO products (id, tenant_id, category_id, name, price, image_url, available) VALUES
  ('22222222-2222-2222-2222-200000000011', '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-200000000001', 'Trà sữa trân châu đường đen', 39000, NULL, true),
  ('22222222-2222-2222-2222-200000000012', '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-200000000001', 'Trà sữa Thái xanh', 35000, NULL, true),
  ('22222222-2222-2222-2222-200000000013', '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-200000000001', 'Trà sữa matcha', 38000, NULL, true),
  ('22222222-2222-2222-2222-200000000014', '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-200000000001', 'Trà sữa khoai môn', 35000, NULL, true),
  ('22222222-2222-2222-2222-200000000015', '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-200000000001', 'Hồng trà sữa', 32000, NULL, true),
  ('22222222-2222-2222-2222-200000000021', '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-200000000002', 'Trà đào cam sả', 42000, NULL, true),
  ('22222222-2222-2222-2222-200000000022', '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-200000000002', 'Trà vải', 38000, NULL, true),
  ('22222222-2222-2222-2222-200000000023', '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-200000000002', 'Trà chanh dây', 35000, NULL, true);

-- Theme xanh ngọc (#0EA5A4) — cố ý khác hẳn đỏ #E23744 của tenant A để chứng minh
-- multi-tenant + SDUI hoạt động thật (docs/31-database.md mục "Mock data cho Stage 0").
INSERT INTO storefronts (tenant_id, schema_version, layout, theme) VALUES (
  '22222222-2222-2222-2222-222222222222',
  3,
  '{
    "screens": {
      "home": {
        "blocks": [
          {"id": "hero_1", "type": "hero_banner", "v": 1,
            "props": {"image_url": "", "aspect_ratio": 1.777, "title": "Trà Sữa Ngọc — tươi mỗi ngày"},
            "data_ref": false},
          {"id": "promo_1", "type": "promo_strip", "v": 1,
            "props": {"text": "Mua 2 tặng 1 mỗi thứ Ba", "bg_token": "primary", "dismissible": true},
            "data_ref": false},
          {"id": "cats_1", "type": "category_row", "v": 1,
            "props": {"style": "circle", "show_all": true},
            "data_ref": "categories"},
          {"id": "grid_trasua", "type": "product_grid", "v": 1,
            "props": {"columns": 2, "card_style": "overlay", "show_price": true},
            "data_ref": "products:22222222-2222-2222-2222-200000000001"},
          {"id": "grid_traitcay", "type": "product_grid", "v": 1,
            "props": {"columns": 2, "card_style": "bordered", "show_price": true},
            "data_ref": "products:22222222-2222-2222-2222-200000000002"}
        ]
      }
    }
  }'::jsonb,
  '{"primary": "#0EA5A4", "surface": "#F5FBFB", "radius_md": 20, "font_family": "Inter"}'::jsonb
);
