import 'package:ds_core/ds_core.dart';

/// Khớp `PublicCategoryDto` (backend/src/main/java/vn/dynamicshop/storefront/dto).
class PublicCategory {
  const PublicCategory({required this.id, required this.name, required this.sortOrder});

  factory PublicCategory.fromJson(Map<String, dynamic> json) => PublicCategory(
        id: json.str('id') ?? '',
        name: json.str('name') ?? '',
        sortOrder: json.integer('sortOrder') ?? 0,
      );

  final String id;
  final String name;
  final int sortOrder;
}
