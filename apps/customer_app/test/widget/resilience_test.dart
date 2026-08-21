import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ds_sdui/ds_sdui.dart';

import '../helpers/load_fixture.dart';

void main() {
  testWidgets('layout chứa block type lạ không có trong registry vẫn render, không crash', (tester) async {
    final fixture = StorefrontFixture.load('all_blocks_smoke'); // block đầu tiên là type lạ cố ý

    // `pumpWidget` là async nên không dùng matcher `returnsNormally` được (nó chỉ bắt lỗi
    // ĐỒNG BỘ) — nếu build ném lỗi, chính `await` dưới đây sẽ làm test fail, không cần
    // bọc thêm. Test này pass tức là đã chứng minh "returns normally".
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(body: StorefrontRenderer(layout: fixture.layout, data: fixture.data)),
    ));
    await tester.pumpAndSettle();

    // Block hợp lệ SAU block lạ vẫn render bình thường — chốt chặn (1) không làm hỏng
    // phần còn lại của màn hình (docs/10-customer-app.md mục "Registry — hai chốt chặn").
    expect(find.text('Quán Test'), findsOneWidget);
  });
}
