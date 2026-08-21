import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ds_sdui/ds_sdui.dart';

import '../helpers/load_fixture.dart';

/// Golden test tiêu biểu (docs/50-qa.md mục "Test SDUI") — mọi fixture trong
/// `test/fixtures/layouts/` phải render đúng như ảnh đã chốt. 2 mock shop thật (theme đỏ
/// vs xanh ngọc — bằng chứng SDUI/theme hoạt động đúng) + 1 fixture tổng hợp phủ 4 block
/// còn lại chưa xuất hiện ở 2 shop thật.
void main() {
  final fixtures = StorefrontFixture.loadAll(['bun_co_ba', 'tra_sua_ngoc', 'all_blocks_smoke']);

  for (final fixture in fixtures) {
    testWidgets('storefront golden: ${fixture.name}', (tester) async {
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(MaterialApp(
        debugShowCheckedModeBanner: false,
        home: Scaffold(body: StorefrontRenderer(layout: fixture.layout, data: fixture.data)),
      ));
      await tester.pumpAndSettle();

      await expectLater(
        find.byType(StorefrontRenderer),
        matchesGoldenFile('goldens/${fixture.name}.png'),
      );
    });
  }
}
