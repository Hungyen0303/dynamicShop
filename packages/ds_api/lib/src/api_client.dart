import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import 'api_exceptions.dart';
import 'create_order_request.dart';
import 'order_response.dart';
import 'storefront_response.dart';

/// Bọc mỏng quanh `http` cho 2 endpoint public plane mà customer_app cần (Stage 0):
/// storefront + tạo đơn. Timeout 10s + retry backoff cho lỗi mạng/5xx
/// (docs/10-customer-app.md mục "Mạng yếu"). Lỗi 4xx (validate, idempotency conflict…)
/// KHÔNG retry — trả thẳng cho caller hiển thị lại cho khách.
class ApiClient {
  ApiClient({required this.baseUrl, http.Client? httpClient, this.timeout = const Duration(seconds: 10)})
      : _client = httpClient ?? http.Client();

  final String baseUrl;
  final Duration timeout;
  final http.Client _client;

  Future<StorefrontResponse> getStorefront(String slug, {int schema = 3}) async {
    final uri = Uri.parse('$baseUrl/v1/s/$slug/storefront?schema=$schema');
    final res = await _sendWithRetry(() => _client.get(uri));
    _throwIfError(res);
    final decoded = _decodeObject(res.body);
    return StorefrontResponse.fromJson(decoded);
  }

  /// `idempotencyKey` do caller sinh (UUID) và GIỮ NGUYÊN khi gọi lại cho cùng một lần
  /// submit — đây là điều kiện để retry tự động ở đây an toàn (server coi 2 request cùng
  /// key + cùng nội dung là một, không tạo đơn thứ hai).
  Future<OrderResponse> createOrder(
    String slug,
    CreateOrderRequest request, {
    required String idempotencyKey,
  }) async {
    final uri = Uri.parse('$baseUrl/v1/s/$slug/orders');
    final body = jsonEncode(request.toJson());
    final res = await _sendWithRetry(() => _client.post(
          uri,
          headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': idempotencyKey,
          },
          body: body,
        ));
    _throwIfError(res);
    final decoded = _decodeObject(res.body);
    return OrderResponse.fromJson(decoded);
  }

  Future<http.Response> _sendWithRetry(Future<http.Response> Function() send, {int maxAttempts = 3}) async {
    Object lastError = const ApiNetworkException('không rõ nguyên nhân');
    for (var attempt = 0; attempt < maxAttempts; attempt++) {
      try {
        final res = await send().timeout(timeout);
        if (res.statusCode < 500) return res; // 2xx/4xx trả thẳng, không retry
        lastError = ApiHttpException(res.statusCode, 'SERVER_ERROR', 'Lỗi máy chủ (${res.statusCode})');
      } on TimeoutException {
        lastError = const ApiTimeoutException();
      } catch (e) {
        lastError = ApiNetworkException(e.toString());
      }
      if (attempt < maxAttempts - 1) {
        await Future.delayed(Duration(milliseconds: 400 * (1 << attempt))); // 400ms, 800ms
      }
    }
    throw lastError;
  }

  void _throwIfError(http.Response res) {
    if (res.statusCode >= 200 && res.statusCode < 300) return;
    var code = 'HTTP_${res.statusCode}';
    var message = 'Lỗi máy chủ (${res.statusCode})';
    try {
      final decoded = jsonDecode(res.body);
      if (decoded is Map<String, dynamic>) {
        code = decoded['code'] is String ? decoded['code'] as String : code;
        message = decoded['message'] is String ? decoded['message'] as String : message;
      }
    } catch (_) {
      // body không phải JSON hợp lệ — giữ message mặc định, không throw thêm lỗi parse
    }
    throw ApiHttpException(res.statusCode, code, message);
  }

  Map<String, dynamic> _decodeObject(String body) {
    final decoded = jsonDecode(body);
    if (decoded is Map<String, dynamic>) return decoded;
    throw const ApiNetworkException('response không phải JSON object');
  }

  void close() => _client.close();
}
