---
name: tenant-isolation
description: Dùng khi viết hoặc sửa bất cứ thứ gì chạm database ở backend — repository, query, entity, migration, bảng mới, hoặc endpoint đọc dữ liệu của tenant. Cũng dùng khi rà soát bảo mật. Chứa cách cô lập tenant đúng, cái bẫy connection pool, và mẫu test bắt buộc.
---

# Cô lập tenant

Rò rỉ tenant là lỗi mang tính sống còn: shop A nhìn thấy đơn của shop B **một lần** là mất cả tỉnh — chủ quán ở tỉnh biết nhau hết.

## Hai tầng

```
Hibernate @TenantId  →  nhanh, tự động, app-level   (cứu 99%)
Postgres RLS         →  người từ chối CUỐI CÙNG      (cứu 1% còn lại)
```

Hibernate filter cứu hầu hết. RLS cứu lúc ai đó viết native query quên `WHERE`.

## 🔴 Cái bẫy connection pool

```java
// ✅ ĐÚNG — gắn transaction, tự reset
jdbc.execute("SET LOCAL app.tenant_id = '" + tenantId + "'");

// ☠️ SAI — gắn session của connection
jdbc.execute("SET app.tenant_id = '" + tenantId + "'");
```

Với `SET` (không `LOCAL`), giá trị bám vào session. HikariCP trả connection về pool; request của tenant B mượn đúng connection đó và vẫn mang `app.tenant_id` của tenant A. **Rò rỉ im lặng** — không log, không exception.

## RLS cho bảng mới

```sql
ALTER TABLE <bảng> ENABLE ROW LEVEL SECURITY;
ALTER TABLE <bảng> FORCE  ROW LEVEL SECURITY;   -- FORCE là bắt buộc

CREATE POLICY tenant_isolation ON <bảng>
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
```

Không có `FORCE`, role owner (thường là role app dùng) bỏ qua policy.

## Admin bypass
Không viết exception vào policy. Dùng DataSource thứ hai với role `app_admin` (`BYPASSRLS`), chỉ inject vào package `admin/`. Tách vật lý thì không thể dùng nhầm.

## Test bắt buộc

```java
@Test
void tenant_b_khong_doc_duoc_du_lieu_cua_a() {
    var a = seedTenantWithOrders(3);
    var b = seedTenantWithOrders(2);
    withTenant(b, () -> {
        assertThat(orderRepo.findAll()).hasSize(2);
        assertThat(orderRepo.findById(a.orderIds().get(0))).isEmpty();  // ← dòng đáng giá nhất
    });
}

@Test
void guc_khong_ro_ri_qua_connection_pool() {
    withTenant(a, () -> orderRepo.findAll());
    withoutTenant(() -> {
        var guc = jdbc.queryForObject(
            "SELECT current_setting('app.tenant_id', true)", String.class);
        assertThat(guc).isNullOrEmpty();
    });
}
```

`findAll` bị filter là đương nhiên. **`findById` mới là đường rò rỉ thật** — ID lộ qua URL, qua log, qua đoán.

⚠️ **Bắt buộc Testcontainers, không dùng H2.** RLS là tính năng Postgres; H2 không có — test sẽ xanh trong khi production rò rỉ.

## Checklist bảng mới
- [ ] `tenant_id UUID NOT NULL`
- [ ] `ENABLE` + `FORCE ROW LEVEL SECURITY` + policy
- [ ] Index có `tenant_id` ở vị trí đầu
- [ ] Đã thêm vào test cô lập tenant
- [ ] Không `nativeQuery` (nếu buộc phải: cần duyệt + test riêng)
