import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ds_sdui/ds_sdui.dart';

void main() {
  testWidgets('layout thiếu theme/field mới vẫn render được (parse khoan dung)', (tester) async {
    final layout = <String, dynamic>{
      'schema_version': 3,
      // 'theme' cố ý thiếu — DsTokens.fromThemeJson phải rơi về default, không throw.
      'screens': {
        'home': {
          'blocks': [
            {
              'id': 'info_1',
              'type': 'info_card',
              'v': 1,
              'props': {'title': 'Bún Cô Ba', 'body': 'Mở cửa 6h-22h'},
            },
          ],
        },
      },
    };

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(body: StorefrontRenderer(layout: layout, data: const {})),
    ));

    expect(find.text('Bún Cô Ba'), findsOneWidget);
  });

  testWidgets('layout chứa block lạ xen giữa block hợp lệ vẫn render trọn phần còn lại', (tester) async {
    final layout = <String, dynamic>{
      'schema_version': 3,
      'theme': {'primary': '#E23744', 'surface': '#FFFFFF', 'radius_md': 12, 'font_family': 'Be Vietnam Pro'},
      'screens': {
        'home': {
          'blocks': [
            {
              'id': 'unknown_1',
              'type': 'brand_new_block_app_chua_biet',
              'v': 1,
              'props': {},
            },
            {
              'id': 'info_1',
              'type': 'info_card',
              'v': 1,
              'props': {'title': 'Vẫn render bình thường', 'body': ''},
            },
          ],
        },
      },
    };

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(body: StorefrontRenderer(layout: layout, data: const {})),
    ));

    expect(find.text('Vẫn render bình thường'), findsOneWidget);
  });

  testWidgets('renderStorefront() — hàm tiện lợi trả về đúng widget dựng được', (tester) async {
    final layout = <String, dynamic>{
      'schema_version': 3,
      'screens': {
        'home': {
          'blocks': [
            {'id': 's1', 'type': 'spacer', 'v': 1, 'props': {}},
          ],
        },
      },
    };

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(body: renderStorefront(layout: layout, data: const {})),
    ));

    expect(tester.takeException(), isNull);
  });
}
