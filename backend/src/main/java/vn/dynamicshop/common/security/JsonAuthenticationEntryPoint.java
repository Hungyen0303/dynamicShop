package vn.dynamicshop.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import vn.dynamicshop.common.error.ErrorResponse;

/**
 * 🔴 Sửa ở sprint 2.1b. Mặc định của Spring Security khi request thiếu xác thực là
 * {@code Http403ForbiddenEntryPoint} → trả **403**. Về mặt HTTP thì sai (403 nghĩa là "đã
 * biết anh là ai, nhưng không cho"), và ở dự án này nó là lỗi mất đơn:
 *
 * Quyết định #12 (progress.md mục 3) dựng toàn bộ vòng đời phiên merchant trên luật "gặp
 * 401 thì tự đăng nhập lại" — không có refresh token. Nếu merchant_app lỡ gửi thiếu header
 * {@code Authorization} (rất dễ xảy ra khi foreground service khởi động lại lúc 3 giờ sáng
 * và đọc token từ đĩa chưa xong), nó sẽ nhận 403, không nằm trong luật re-login, nên
 * **im lặng ngừng nhận đơn** cho tới khi có người mở app lên xem.
 *
 * Nay mọi request thiếu/sai xác thực đều ra 401 với cùng shape lỗi {@link ErrorResponse}
 * như phần còn lại của API, để client chỉ cần nhớ đúng MỘT luật.
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        writeUnauthenticated(objectMapper, response, "Thiếu hoặc sai thông tin xác thực — hãy đăng nhập lại");
    }

    /**
     * Dùng chung với {@link JwtAuthenticationFilter} để hai đường (thiếu header / token hỏng)
     * trả về y hệt nhau. Client không cần phân biệt hai trường hợp: cả hai đều là "đăng nhập
     * lại đi".
     */
    static void writeUnauthenticated(ObjectMapper objectMapper, HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse("UNAUTHENTICATED", message));
    }
}
