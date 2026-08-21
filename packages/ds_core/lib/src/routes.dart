/// Routing tối thiểu — chỉ tên route dùng chung, không phải router package. Giỏ hàng /
/// checkout / theo dõi đơn nhận tham số runtime nên `customer_app` tự `Navigator.push`
/// trực tiếp; các hằng số này chỉ dùng để log/telemetry nhất quán tên màn hình.
class AppRoutes {
  const AppRoutes._();

  static const home = 'home';
  static const cart = 'cart';
  static const checkout = 'checkout';
  static const orderTracking = 'order_tracking';
  static const devShopSwitcher = 'dev_shop_switcher';
}
