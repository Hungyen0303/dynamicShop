/// `contracts/tokens.json` → `typography.scale.options`. Chỉ hai giá trị này được phép.
enum DsTypographyScale {
  compact,
  comfortable;

  static DsTypographyScale fromString(String? raw) => switch (raw) {
        'comfortable' => DsTypographyScale.comfortable,
        _ => DsTypographyScale.compact, // default theo tokens.json
      };

  /// Hệ số nhân cỡ chữ — không có trong contract (chỉ liệt kê 2 option), đây là lựa chọn
  /// mặc định hợp lý của Stage 0, cần hỏi designer nếu cần chỉnh chính xác hơn.
  double get textScaleFactor => this == DsTypographyScale.comfortable ? 1.125 : 1.0;
}
