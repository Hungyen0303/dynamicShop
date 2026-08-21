import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ds_tokens/ds_tokens.dart';

void main() {
  group('DsTokens.fromThemeJson — khoan dung', () {
    test('theme rỗng vẫn parse được, dùng app default', () {
      final t = DsTokens.fromThemeJson(const {});
      expect(t.primary, DsDefaults.primary);
      expect(t.surface, DsDefaults.surface);
      expect(t.radiusMd, DsDefaults.radiusMd);
      expect(t.fontFamily, DsDefaults.fontFamily);
    });

    test('parse đúng theme thật của mock shop bun-co-ba', () {
      final t = DsTokens.fromThemeJson(const {
        'primary': '#E23744',
        'surface': '#FFFFFF',
        'radius_md': 12,
        'font_family': 'Be Vietnam Pro',
      });
      expect(t.primary, const Color(0xFFE23744));
      expect(t.radiusMd, 12);
      expect(t.fontFamily, 'Be Vietnam Pro');
    });

    test('hex sai format rơi về default, không throw', () {
      final t = DsTokens.fromThemeJson(const {'primary': 'not-a-hex-color'});
      expect(t.primary, DsDefaults.primary);
    });

    test('font không nằm trong allowed_fonts bị từ chối, rơi về default', () {
      final t = DsTokens.fromThemeJson(const {'font_family': 'Comic Sans Free Download'});
      expect(t.fontFamily, DsDefaults.fontFamily);
    });

    test('radius_md gửi dạng int (không phải double) vẫn parse được', () {
      final t = DsTokens.fromThemeJson(const {'radius_md': 20});
      expect(t.radiusMd, 20.0);
    });

    test('field không configurable (border, danger…) luôn dùng app default dù server gửi', () {
      final t = DsTokens.fromThemeJson(const {'border': '#000000', 'danger': '#000000'});
      expect(t.border, DsDefaults.border);
      expect(t.danger, DsDefaults.danger);
    });
  });

  group('Màu chữ tự suy', () {
    test('nền sáng -> chữ tối, nền tối -> chữ trắng', () {
      final t = DsTokens.appDefault();
      expect(t.onColorOf(const Color(0xFFFFFFFF)), const Color(0xFF1A1A1A));
      expect(t.onColorOf(const Color(0xFF000000)), Colors.white);
    });
  });

  group('Spacing', () {
    test('space() luôn là bội số của spacingUnit', () {
      final t = DsTokens.appDefault();
      expect(t.space(2), 16);
      expect(t.space(0.5), 4);
    });
  });
}
