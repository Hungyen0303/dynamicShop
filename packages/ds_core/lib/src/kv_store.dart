import 'package:shared_preferences/shared_preferences.dart';

/// Bọc mỏng quanh `shared_preferences` cho các giá trị đơn giản: slug shop đang chọn (màn
/// hình dev đổi shop), Idempotency-Key đang chờ gửi lại của đơn hiện tại, giỏ hàng thô…
/// Không đặt logic nghiệp vụ ở đây — chỉ đọc/ghi.
class KvStore {
  const KvStore();

  Future<String?> getString(String key) async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(key);
  }

  Future<void> setString(String key, String value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(key, value);
  }

  Future<void> remove(String key) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(key);
  }
}
