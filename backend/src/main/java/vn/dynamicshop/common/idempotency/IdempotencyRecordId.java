package vn.dynamicshop.common.idempotency;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Khoá kép (key, tenant_id) — khớp {@code idempotency_keys} trong V1__init.sql. */
public class IdempotencyRecordId implements Serializable {

    private String key;
    private UUID tenantId;

    protected IdempotencyRecordId() {
    }

    public IdempotencyRecordId(String key, UUID tenantId) {
        this.key = key;
        this.tenantId = tenantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IdempotencyRecordId that)) {
            return false;
        }
        return Objects.equals(key, that.key) && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, tenantId);
    }
}
