package vn.dynamicshop.common.security;

import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT tự cấp, tự viết — Stage 0 KHÔNG dùng provider ngoài (docs/70-stages.md "Auth ở
 * Stage 0"). Chỉ HS256 bằng {@code javax.crypto} sẵn có trong JDK, không thêm thư viện
 * JWT nào (jjwt/nimbus...) — dependency mới cần người duyệt (AGENTS.md mục 7).
 */
@Component
public class JwtService {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String HMAC_ALGO = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final byte[] secretKey;
    private final long expirationMinutes;

    public JwtService(ObjectMapper objectMapper,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.objectMapper = objectMapper;
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMinutes = expirationMinutes;
    }

    public String issue(Map<String, Object> claims) {
        Map<String, Object> body = new LinkedHashMap<>(claims);
        Instant now = Instant.now();
        body.put("iat", now.getEpochSecond());
        body.put("exp", now.plus(expirationMinutes, ChronoUnit.MINUTES).getEpochSecond());

        String headerPart = B64.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String payloadPart = encodeJson(body);
        String signingInput = headerPart + "." + payloadPart;
        String signature = B64.encodeToString(hmac(signingInput));
        return signingInput + "." + signature;
    }

    /** Verify chữ ký + hạn dùng, trả về claims nếu hợp lệ. */
    public Map<String, Object> verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidJwtException("Sai định dạng JWT");
        }
        String signingInput = parts[0] + "." + parts[1];
        byte[] expectedSig = hmac(signingInput);
        byte[] actualSig = B64D.decode(parts[2]);
        if (!java.security.MessageDigest.isEqual(expectedSig, actualSig)) {
            throw new InvalidJwtException("Chữ ký JWT không khớp");
        }
        Map<String, Object> claims = decodeJson(parts[1]);
        Object exp = claims.get("exp");
        if (exp instanceof Number expSeconds && Instant.now().getEpochSecond() > expSeconds.longValue()) {
            throw new InvalidJwtException("JWT đã hết hạn");
        }
        return claims;
    }

    private String encodeJson(Map<String, Object> body) {
        try {
            return B64.encodeToString(objectMapper.writeValueAsBytes(body));
        } catch (Exception e) {
            throw new IllegalStateException("Không encode được JWT payload", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeJson(String base64Part) {
        try {
            return objectMapper.readValue(B64D.decode(base64Part), Map.class);
        } catch (Exception e) {
            throw new InvalidJwtException("Không đọc được JWT payload");
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALGO));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Không tạo được chữ ký JWT", e);
        }
    }

    public static class InvalidJwtException extends RuntimeException {
        public InvalidJwtException(String message) {
            super(message);
        }
    }
}
