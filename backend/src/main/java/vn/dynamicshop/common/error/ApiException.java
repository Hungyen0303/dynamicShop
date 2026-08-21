package vn.dynamicshop.common.error;

import org.springframework.http.HttpStatus;

/** Base cho exception nghiệp vụ muốn tự chọn HTTP status — {@link GlobalExceptionHandler} bắt và map. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
