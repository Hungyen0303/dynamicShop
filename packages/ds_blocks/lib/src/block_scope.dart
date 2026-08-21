import 'sdui_action.dart';

/// Những gì một block widget cần ngoài `BlockNode` — data đã resolve sẵn (server trả
/// cùng lúc với layout, bất biến #6 "một round-trip") và callback điều hướng. Block
/// KHÔNG tự gọi API (bất biến "Pure, không tự gọi network").
class BlockScope {
  const BlockScope({this.data, this.onAction});

  /// Dữ liệu ứng với `node.dataRef` — `List<PublicCategory>`, `List<PublicProduct>`, hoặc
  /// `null` nếu block không cần data / ref không resolve được. Kiểu cụ thể do từng block
  /// widget tự `is`-check, không ép kiểu cứng ở tầng này.
  final dynamic data;

  final void Function(SduiAction action)? onAction;
}
