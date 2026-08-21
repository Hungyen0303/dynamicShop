/// Flavor CHỈ chứa danh tính (tên app, tenant mặc định) — không bao giờ menu/giá/theme
/// (bất biến #1 docs/10-customer-app.md, bất biến 9b AGENTS.md). Ba `main_*.dart` set
/// đúng một instance của class này trước khi gọi `runApp`.
class AppConfig {
  const AppConfig({required this.flavor, required this.apiBaseUrl, required this.defaultTenantSlug});

  final String flavor;
  final String apiBaseUrl;

  /// Slug hiện lên đầu tiên khi app mở lần đầu (chưa có gì lưu trong `KvStore`). Người
  /// dùng thật (chủ quán) không thấy màn hình đổi shop — chỉ có ở `dev` để demo/test theo
  /// checklist "Xong khi" (đổi giữa 2 mock shop).
  final String defaultTenantSlug;

  bool get isDev => flavor == 'dev';

  static late AppConfig instance;
}
