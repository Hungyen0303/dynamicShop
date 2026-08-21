package vn.dynamicshop.common.idempotency;

import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.dynamicshop.common.error.ApiException;
import vn.dynamicshop.common.tenant.TenantContext;

/**
 * Bất biến #9 (docs/30-backend.md mục "Idempotency"). Chạy TRONG CÙNG transaction với
 * hành động tạo thực thể — nếu action rollback thì bản ghi idempotency cũng rollback theo,
 * tránh tình huống "đơn không tạo được nhưng idempotency key coi như đã dùng".
 *
 * Giới hạn đã biết: không khoá phân tán chống hai request TRÙNG key gửi ĐỒNG THỜI (race
 * hiếm ở quy mô Stage 0) — insert thứ hai sẽ vi phạm PRIMARY KEY (key, tenant_id) và ném
 * lỗi thay vì trả lại response cũ. Chấp nhận được cho local/1 shop; ghi chú lại nếu cần
 * làm chắc hơn ở Stage sau.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public <T> ResponseEntity<T> execute(String idempotencyKey, Object requestPayload, Class<T> responseType,
            Supplier<ResponseEntity<T>> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }
        var tenantId = TenantContext.require();
        String requestHash = sha256(writeJson(requestPayload));
        var id = new IdempotencyRecordId(idempotencyKey, tenantId);

        return repository.findById(id)
                .map(existing -> replay(existing, requestHash, responseType))
                .orElseGet(() -> executeAndRecord(idempotencyKey, tenantId, requestHash, action));
    }

    private <T> ResponseEntity<T> replay(IdempotencyRecord existing, String requestHash, Class<T> responseType) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException();
        }
        T body = objectMapper.convertValue(existing.getResponseBody(), responseType);
        return ResponseEntity.status(existing.getResponseStatus()).body(body);
    }

    private <T> ResponseEntity<T> executeAndRecord(String key, java.util.UUID tenantId, String requestHash,
            Supplier<ResponseEntity<T>> action) {
        ResponseEntity<T> response = action.get();
        @SuppressWarnings("unchecked")
        Map<String, Object> bodyAsMap = objectMapper.convertValue(response.getBody(), Map.class);
        repository.save(new IdempotencyRecord(key, tenantId, requestHash, response.getStatusCode().value(),
                bodyAsMap));
        return response;
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Không serialize được request để tính idempotency hash", e);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class MissingIdempotencyKeyException extends ApiException {
        public MissingIdempotencyKeyException() {
            super(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", "Thiếu header Idempotency-Key");
        }
    }

    public static class IdempotencyConflictException extends ApiException {
        public IdempotencyConflictException() {
            super(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT",
                    "Idempotency-Key đã dùng cho một request khác nội dung");
        }
    }
}
