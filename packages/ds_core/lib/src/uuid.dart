import 'dart:math';

/// UUID v4 tự sinh — không thêm dependency `uuid` chỉ cho một hàm ngắn. Dùng cho
/// `Idempotency-Key` khi tạo đơn (docs/10-customer-app.md mục "Mạng yếu": "UUID client
/// sinh, giữ nguyên khi retry").
String generateUuidV4() {
  final rnd = Random.secure();
  final bytes = List<int>.generate(16, (_) => rnd.nextInt(256));
  bytes[6] = (bytes[6] & 0x0f) | 0x40; // version 4
  bytes[8] = (bytes[8] & 0x3f) | 0x80; // variant 10xxxxxx

  String hex(int start, int end) =>
      bytes.sublist(start, end).map((b) => b.toRadixString(16).padLeft(2, '0')).join();

  return '${hex(0, 4)}-${hex(4, 6)}-${hex(6, 8)}-${hex(8, 10)}-${hex(10, 16)}';
}
