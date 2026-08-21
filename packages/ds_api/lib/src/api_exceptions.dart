/// Lỗi từ mạng (mất kết nối, DNS…) — khác lỗi HTTP có response (xem [ApiHttpException]).
/// Caller (customer_app) bắt lỗi này để rơi xuống fallback tầng 2/3
/// (docs/10-customer-app.md mục "Fallback ba tầng").
class ApiNetworkException implements Exception {
  const ApiNetworkException(this.message);
  final String message;
  @override
  String toString() => 'ApiNetworkException: $message';
}

class ApiTimeoutException implements Exception {
  const ApiTimeoutException();
  @override
  String toString() => 'ApiTimeoutException: quá 10s không phản hồi';
}

/// Server trả lỗi có body `{code, message}` (khớp `ErrorResponse` backend) — ví dụ
/// `IDEMPOTENCY_KEY_CONFLICT`, `MISSING_IDEMPOTENCY_KEY`, `STOREFRONT_NOT_CONFIGURED`.
class ApiHttpException implements Exception {
  const ApiHttpException(this.statusCode, this.code, this.message);
  final int statusCode;
  final String code;
  final String message;
  @override
  String toString() => 'ApiHttpException($statusCode): $code — $message';
}
