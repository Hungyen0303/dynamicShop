/// Parse JSON khoan dung — docs/10-customer-app.md mục "Parse JSON — luôn khoan dung".
///
/// Lý do cụ thể: thêm field mới ⇒ config của N shop đã lưu trong DB không có field đó
/// ⇒ parser nghiêm khắc làm N shop trắng màn hình cùng lúc. Không bao giờ dùng `as double`
/// hay `!` khi đọc props từ server — luôn qua các hàm dưới đây.
extension Json on Map<String, dynamic> {
  String? str(String k) => this[k] is String ? this[k] as String : null;

  // Cố ý dùng `is num` thay vì `this[k] as num?` (khác nguyên văn docs/10-customer-app.md)
  // — `as num?` chỉ an toàn khi giá trị là `null`, nhưng THROW nếu field tồn tại với SAI
  // kiểu (ví dụ server/DB cũ lưu String cho field lẽ ra là số). "Khoan dung tuyệt đối"
  // nghĩa là sai kiểu cũng phải rơi về `null`, không riêng gì thiếu field.
  double? dbl(String k) {
    final v = this[k];
    return v is num ? v.toDouble() : null; // nhận cả int lẫn double
  }

  int? integer(String k) {
    final v = this[k];
    return v is num ? v.toInt() : null;
  }

  bool bl(String k, {bool or = false}) => this[k] is bool ? this[k] as bool : or;

  /// Danh sách khoan dung — field thiếu hoặc sai kiểu trả về rỗng, không throw.
  List<dynamic> list(String k) => this[k] is List ? this[k] as List : const [];

  /// Map con khoan dung — field thiếu hoặc sai kiểu trả về rỗng, không throw.
  /// Đặt tên `obj` (không phải `map`) vì `Map` đã có instance method `map()` riêng
  /// (transform entries) — trùng tên sẽ bị Dart ưu tiên method có sẵn, âm thầm sai kiểu.
  Map<String, dynamic> obj(String k) =>
      this[k] is Map ? Map<String, dynamic>.from(this[k] as Map) : const {};
}
