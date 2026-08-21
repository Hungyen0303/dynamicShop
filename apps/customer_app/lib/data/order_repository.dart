import 'package:ds_api/ds_api.dart';
import 'package:ds_core/ds_core.dart';

/// Đặt đơn — HARDCODE, không SDUI (bất biến #5). `Idempotency-Key` được sinh MỘT LẦN cho
/// một phiên đặt hàng và GIỮ NGUYÊN khi retry (docs/10-customer-app.md mục "Mạng yếu") —
/// lưu qua `KvStore` để sống sót cả khi app bị kill giữa lúc đang gửi (ví dụ mất mạng 30s
/// rồi app bị hệ thống thu hồi bộ nhớ, một tình huống bình thường trên máy tầm thấp).
class OrderRepository {
  OrderRepository({required this.apiClient, KvStore? kvStore}) : _kv = kvStore ?? const KvStore();

  final ApiClient apiClient;
  final KvStore _kv;

  String _keyFor(String slug) => 'pending_idempotency_key_$slug';

  Future<String> _idempotencyKeyFor(String slug) async {
    final existing = await _kv.getString(_keyFor(slug));
    if (existing != null) return existing;
    final fresh = generateUuidV4();
    await _kv.setString(_keyFor(slug), fresh);
    return fresh;
  }

  Future<OrderResponse> submit(String slug, CreateOrderRequest request) async {
    final key = await _idempotencyKeyFor(slug);
    final response = await apiClient.createOrder(slug, request, idempotencyKey: key);
    // Đơn đã tạo xong — phiên đặt hàng kết thúc, lần bấm "Đặt hàng" tiếp theo là một đơn
    // MỚI nên cần key mới, không phải reuse key cũ (sẽ bị server coi là replay đơn cũ).
    await _kv.remove(_keyFor(slug));
    return response;
  }
}
