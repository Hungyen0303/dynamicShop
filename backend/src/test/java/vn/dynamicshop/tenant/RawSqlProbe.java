package vn.dynamicshop.tenant;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bean HỖ TRỢ TEST DUY NHẤT — mô phỏng chính xác "ai đó viết native query quên WHERE"
 * (.claude/skills/tenant-isolation/SKILL.md). Chủ ý KHÔNG dùng repository/Hibernate
 * {@code @TenantId} — SELECT thẳng bằng JdbcTemplate, không mệnh đề WHERE nào theo
 * tenant. Nếu RLS hoạt động đúng, Postgres tự cắt kết quả theo GUC {@code app.tenant_id}
 * dù code Java không hề lọc.
 *
 * Có {@code @Transactional} để đi qua đúng proxy Spring (không self-invocation) — nhờ đó
 * {@code TenantAwareJpaTransactionManager} vẫn phát SET LOCAL app.tenant_id đúng lúc
 * transaction mở, y hệt mọi request thật. Bài test dùng bean này chỉ để xoá tầng lọc
 * Hibernate, KHÔNG xoá tầng GUC.
 */
@Component
public class RawSqlProbe {

    private final JdbcTemplate jdbcTemplate;

    public RawSqlProbe(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> selectAllOrdersRaw() {
        return jdbcTemplate.queryForList("SELECT id, tenant_id, code FROM orders");
    }
}
