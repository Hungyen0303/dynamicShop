import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:ds_core/ds_core.dart';

void main() {
  test('load() trên slug chưa từng lưu trả về null nhanh, không treo', () async {
    final tempDir = await Directory.systemTemp.createTemp('ds_cache_unit_test_');
    addTearDown(() => tempDir.delete(recursive: true));
    final cache = StorefrontDiskCache(directoryProvider: () async => tempDir);

    final result = await cache.load('khong-ton-tai', 3);

    expect(result, isNull);
  });

  test('save() rồi load() trả về đúng dữ liệu', () async {
    final tempDir = await Directory.systemTemp.createTemp('ds_cache_unit_test_');
    addTearDown(() => tempDir.delete(recursive: true));
    final cache = StorefrontDiskCache(directoryProvider: () async => tempDir);

    await cache.save('bun-co-ba', 3, '{"layout":{"a":1},"data":{}}');
    final result = await cache.load('bun-co-ba', 3);

    expect(result, isNotNull);
    expect(result!['layout'], {'a': 1});
  });
}
