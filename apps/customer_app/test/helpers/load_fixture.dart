import 'dart:convert';
import 'dart:io';

/// Đọc fixture layout thật trong `test/fixtures/layouts/` — dùng `dart:io` thoải mái vì
/// test chạy trên máy host (VM), không phải trên thiết bị/web (khác `ds_blocks`/`ds_sdui`
/// vốn phải build được cho web).
class StorefrontFixture {
  const StorefrontFixture({required this.name, required this.layout, required this.data});

  final String name;
  final Map<String, dynamic> layout;
  final Map<String, dynamic> data;

  static StorefrontFixture load(String name) {
    final file = File('test/fixtures/layouts/$name.json');
    final decoded = jsonDecode(file.readAsStringSync()) as Map<String, dynamic>;
    return StorefrontFixture(
      name: name,
      layout: Map<String, dynamic>.from(decoded['layout'] as Map),
      data: Map<String, dynamic>.from(decoded['data'] as Map),
    );
  }

  static List<StorefrontFixture> loadAll(List<String> names) => names.map(load).toList();
}
