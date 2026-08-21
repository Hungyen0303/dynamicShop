/// Variant = preset trong map, KHÔNG phải widget riêng (docs/60-design.md mục "Variant =
/// preset, không phải class"). `product_grid`/`product_list` dùng chung một
/// `ProductCard` + preset này — thêm variant thứ 5 = thêm một dòng trong [presets].
class ProductCardStyle {
  const ProductCardStyle({
    this.radius,
    this.elevation = 0,
    this.showBorder = false,
    this.overlayText = false,
  });

  /// `null` nghĩa là "không có ý kiến ở tầng variant" — `StyleResolver` sẽ rơi tiếp xuống
  /// tenantTheme/appDefault, không phải `0`.
  final double? radius;
  final double elevation;
  final bool showBorder;
  final bool overlayText;

  static const presets = <String, ProductCardStyle>{
    'minimal': ProductCardStyle(radius: 0, elevation: 0),
    'elevated': ProductCardStyle(radius: 12, elevation: 2),
    'bordered': ProductCardStyle(radius: 8, showBorder: true),
    'overlay': ProductCardStyle(radius: 16, overlayText: true),
  };

  static ProductCardStyle of(String? key) => presets[key] ?? presets['elevated']!;
}
