-- Mock tenant A — quán bún, theme đỏ. Chỉ nạp ở profile local (xem application.yml
-- spring.flyway.locations theo profile). UUID cố định (không gen_random_uuid()) để
-- storefronts.layout tham chiếu đúng category_id trong data_ref "products:{category_id}".

INSERT INTO tenants (id, slug, name, status, timezone, business_day_start)
VALUES ('11111111-1111-1111-1111-111111111111', 'bun-co-ba', 'Bún Cô Ba', 'ACTIVE', 'Asia/Ho_Chi_Minh', '04:00');

-- Mật khẩu fixture: bunca123 (BCrypt, xem backend/docs cho cách tạo lại nếu cần đổi)
INSERT INTO merchants (id, tenant_id, phone, password_hash, name, role)
VALUES ('11111111-1111-1111-1111-900000000001', '11111111-1111-1111-1111-111111111111',
        '0901000001', '$2a$10$eltF/59ibHuDEnzPhIloV.cqZw1S3bahaOPBfMZTfibJ91RHUBmb6', 'Cô Ba', 'OWNER');

INSERT INTO categories (id, tenant_id, name, sort_order) VALUES
  ('11111111-1111-1111-1111-100000000001', '11111111-1111-1111-1111-111111111111', 'Bún', 1),
  ('11111111-1111-1111-1111-100000000002', '11111111-1111-1111-1111-111111111111', 'Cơm', 2),
  ('11111111-1111-1111-1111-100000000003', '11111111-1111-1111-1111-111111111111', 'Nước uống', 3);

INSERT INTO products (id, tenant_id, category_id, name, price, image_url, available) VALUES
  ('11111111-1111-1111-1111-100000000011', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-100000000001', 'Bún bò Huế', 45000, NULL, true),
  ('11111111-1111-1111-1111-100000000012', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-100000000001', 'Bún riêu cua', 40000, NULL, true),
  ('11111111-1111-1111-1111-100000000013', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-100000000001', 'Bún chả Hà Nội', 42000, NULL, true),
  ('11111111-1111-1111-1111-100000000014', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-100000000001', 'Bún mọc', 38000, NULL, true),
  ('11111111-1111-1111-1111-100000000015', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-100000000001', 'Bún ốc chuối đậu', 40000, NULL, true),
  ('11111111-1111-1111-1111-100000000021', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-100000000002', 'Cơm tấm sườn bì chả', 45000, NULL, true),
  ('11111111-1111-1111-1111-100000000022', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-100000000002', 'Cơm gà xối mỡ', 42000, NULL, true),
  ('11111111-1111-1111-1111-100000000023', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-100000000002', 'Cơm sườn nướng', 40000, NULL, true),
  ('11111111-1111-1111-1111-100000000024', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-100000000002', 'Cơm chay thập cẩm', 35000, NULL, true),
  ('11111111-1111-1111-1111-100000000031', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-100000000003', 'Trà đá', 5000, NULL, true),
  ('11111111-1111-1111-1111-100000000032', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-100000000003', 'Nước sâm', 15000, NULL, true),
  ('11111111-1111-1111-1111-100000000033', '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-100000000003', 'Nước chanh dây', 18000, NULL, true);

-- Theme đỏ (#E23744 = contracts/tokens.json colors.primary.default) — chứng minh tenant A
-- khác hẳn tenant B (theme xanh) dù chạy chung một backend.
INSERT INTO storefronts (tenant_id, schema_version, layout, theme) VALUES (
  '11111111-1111-1111-1111-111111111111',
  3,
  '{
    "screens": {
      "home": {
        "blocks": [
          {"id": "hero_1", "type": "hero_banner", "v": 1,
            "props": {"image_url": "", "aspect_ratio": 1.777, "title": "Bún Cô Ba — quán nhà làm"},
            "data_ref": false},
          {"id": "cats_1", "type": "category_row", "v": 1,
            "props": {"style": "chip", "show_all": true},
            "data_ref": "categories"},
          {"id": "grid_bun", "type": "product_grid", "v": 1,
            "props": {"columns": 2, "card_style": "elevated", "show_price": true},
            "data_ref": "products:11111111-1111-1111-1111-100000000001"},
          {"id": "grid_com", "type": "product_grid", "v": 1,
            "props": {"columns": 2, "card_style": "elevated", "show_price": true},
            "data_ref": "products:11111111-1111-1111-1111-100000000002"},
          {"id": "grid_nuoc", "type": "product_grid", "v": 1,
            "props": {"columns": 3, "card_style": "minimal", "show_price": true},
            "data_ref": "products:11111111-1111-1111-1111-100000000003"}
        ]
      }
    }
  }'::jsonb,
  '{"primary": "#E23744", "surface": "#FFFFFF", "radius_md": 12, "font_family": "Be Vietnam Pro"}'::jsonb
);
