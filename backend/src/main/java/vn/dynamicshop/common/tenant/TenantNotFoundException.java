package vn.dynamicshop.common.tenant;

import org.springframework.http.HttpStatus;
import vn.dynamicshop.common.error.ApiException;

public class TenantNotFoundException extends ApiException {
    public TenantNotFoundException(String slug) {
        super(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Không tìm thấy shop: " + slug);
    }
}
