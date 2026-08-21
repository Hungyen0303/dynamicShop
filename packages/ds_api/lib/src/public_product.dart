import 'package:ds_core/ds_core.dart';

/// Khớp `PublicProductDto`. `price` là số nguyên đồng (bất biến #3 AGENTS.md — không bao
/// giờ `double`). `imageUrl` có thể `null` — nơi hiển thị phải tự có placeholder
/// (docs/60-design.md mục "Ảnh"), không phải trách nhiệm của model này.
class PublicProduct {
  const PublicProduct({
    required this.id,
    required this.name,
    required this.price,
    required this.imageUrl,
    required this.available,
  });

  factory PublicProduct.fromJson(Map<String, dynamic> json) => PublicProduct(
        id: json.str('id') ?? '',
        name: json.str('name') ?? '',
        price: json.integer('price') ?? 0,
        imageUrl: json.str('imageUrl'),
        available: json.bl('available', or: true),
      );

  final String id;
  final String name;
  final int price;
  final String? imageUrl;
  final bool available;
}
