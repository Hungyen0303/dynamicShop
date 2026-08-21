import 'package:ds_core/ds_core.dart';

/// Một block phẳng trong `screens.home.blocks[]` — khớp `$defs/block` của
/// `contracts/storefront.schema.json`. Parse khoan dung tuyệt đối: field thiếu/sai kiểu
/// không throw, dùng giá trị rỗng an toàn — một block cấu hình sai không được kéo sập cả
/// layout lúc PARSE (registry còn có thêm một lớp chặn nữa lúc BUILD, xem `ds_sdui`).
class BlockNode {
  const BlockNode({
    required this.id,
    required this.type,
    required this.v,
    required this.props,
    this.dataRef,
    this.overrides = const {},
  });

  factory BlockNode.fromJson(Map<String, dynamic> json) {
    final rawDataRef = json['data_ref'];
    return BlockNode(
      id: json.str('id') ?? '',
      type: json.str('type') ?? '',
      v: json.integer('v') ?? 1,
      props: json.obj('props'),
      // schema: data_ref là string hoặc `false` — chỉ giữ lại khi là string.
      dataRef: rawDataRef is String ? rawDataRef : null,
      overrides: json.obj('overrides'),
    );
  }

  final String id;
  final String type;
  final int v;
  final Map<String, dynamic> props;
  final String? dataRef;
  final Map<String, dynamic> overrides;
}
