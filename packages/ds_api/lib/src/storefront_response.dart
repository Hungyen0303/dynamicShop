import 'package:ds_core/ds_core.dart';

/// Khớp `StorefrontResponseDto` — `GET /v1/s/{slug}/storefront?schema=3`. Cố ý giữ
/// `layout`/`data` là `Map<String, dynamic>` thô, KHÔNG parse tiếp thành model block ở
/// đây — đó là việc của `ds_sdui` (registry mới biết type nào tồn tại). Giữ ranh giới này
/// để `ds_api` không phải biết gì về SDUI, đúng như backend (`StorefrontResponseDto` cũng
/// dùng `Map<String, Object>`).
class StorefrontResponse {
  const StorefrontResponse({required this.layout, required this.data});

  factory StorefrontResponse.fromJson(Map<String, dynamic> json) => StorefrontResponse(
        layout: json.obj('layout'),
        data: json.obj('data'),
      );

  final Map<String, dynamic> layout;
  final Map<String, dynamic> data;
}
