# 02 — Mobile: Merchant App

*Tech spec — DynamicShop, 07/2026*

---

## Nguyên tắc: không cần đẹp, cần **không bao giờ sót đơn**

Đây là app quan trọng nhất hệ thống, dù nó là app ít được chăm chút giao diện nhất.

```
Khách     →  mở app ~2 lần/tuần
Chủ shop  →  mở app ~50 lần/ngày
```

Chất lượng merchant app **chính là** retention tháng 3 — chỉ số sống còn của cả công ty. Customer app xấu thì khách vẫn đặt được. Merchant app sót một đơn lúc 7h tối là mất quán đó.

**Phân bổ công sức ngược trực giác thông thường: merchant app nhiều hơn customer app.**

### Phạm vi đã chốt

| Có | Không có |
|---|---|
| Nhận đơn, chuông, in bill | UI studio / layout editor |
| Báo cáo (thiết kế cho mobile) | Kéo thả block |
| Sửa giá, hết món, giờ mở cửa | Chỉnh theme phức tạp |
| **Cấu hình UI ở mức visibility** (ẩn/hiện block) | Chỉnh radius, màu từng block |

Layout đầy đủ làm trên web studio, và người làm là operator chứ không phải chủ quán.

---

## Android trước, iOS sau — và vì sao

⚠️ **Trên iOS, app không thể đảm bảo kêu chuông khi có đơn mới lúc ở background.** Apple chặn; Critical Alerts cần entitlement đặc biệt rất khó xin và thường bị từ chối.

Thực tế thị trường: gần như toàn bộ chủ quán ở tỉnh dùng Android. **Ship Android trước, iOS để sau hoặc dùng web.** Đừng phí 2 tháng chiến đấu với Apple để phục vụ 3% khách hàng.

---

## 🔴 Vấn đề số 1 ở Việt Nam: OEM giết app nền

Đây là nguyên nhân hàng đầu khiến app bán hàng ở VN sót thông báo, và nó **không xuất hiện trên máy Pixel hay emulator** — bạn sẽ không phát hiện ra cho tới khi chủ quán gọi điện.

Xiaomi (MIUI), Oppo (ColorOS), Vivo (FuntouchOS), Realme, Samsung đều có lớp quản lý pin riêng, mạnh tay hơn Android gốc rất nhiều. Chúng sẽ:
- Kill foreground service sau vài giờ
- Chặn FCM khi app bị "đóng băng"
- Không cho autostart sau khi khởi động lại máy

**Bắt buộc làm cả bốn:**

1. **Xin miễn tối ưu pin** — `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, hiện dialog ngay trong onboarding
2. **Hướng dẫn autostart theo hãng máy** — detect `Build.MANUFACTURER`, mở thẳng màn hình cài đặt tương ứng kèm ảnh minh hoạ từng bước
3. **Foreground service** với notification thường trực ("Đang nhận đơn") — persistent notification là cái giá phải trả để sống sót
4. **Màn hình tự kiểm tra** trong app: *"Trạng thái nhận đơn: ✅ Bình thường / ⚠️ Có thể bị chặn"* — chủ động phát hiện thay vì chờ mất đơn

Điểm 2 và 4 là thứ tách app dùng được khỏi app bị bỏ. Đầu tư vào chúng như một tính năng chính, không phải việc phụ.

---

## Kiến trúc nhận đơn: hai kênh, không tin vào một cái nào

Đơn bị sót là lỗi tệ nhất trong toàn hệ thống. Nên đừng phụ thuộc một cơ chế duy nhất:

| Trạng thái app | Cơ chế |
|---|---|
| Background / khoá màn hình | **FCM data message** → đánh thức foreground service → chuông |
| Foreground | **Polling mỗi 15–20 giây** |
| Mở lại app sau khi mất mạng | Full sync theo `updated_after` |

**Đừng dùng WebSocket ở v1.** Trên mạng 3G tỉnh, WebSocket rớt liên tục và bạn sẽ mất hàng tuần cho reconnect/backoff/heartbeat. Polling "ngu ngốc" nhưng cực bền: mất mạng 30 giây rồi có lại → lần poll sau tự đồng bộ, không cần code gì thêm.

Tốn băng thông hơn? Có. Rẻ hơn nhiều so với một đơn bị sót.

**Dedupe:** push là at-least-once (outbox pattern phía BE), nên client phải chịu được thông báo trùng — dedupe theo `order_id`.

### Chuông phải khó bỏ qua

Quán ăn lúc 7h tối rất ồn. Chuông mặc định của Android sẽ bị bỏ lỡ.

- **Lặp âm thanh cho tới khi có người bấm xác nhận**, không kêu một lần
- Âm lượng theo stream ALARM, không phải NOTIFICATION (ALARM không bị chế độ im lặng tắt)
- Full-screen intent để hiện đè lên màn hình khoá
- Rung kèm
- Cho phép chủ quán chọn âm thanh và test ngay trong cài đặt

---

## In bill K80 — cách tiếp cận

**Thử package sẵn trước khi viết native.** Mỗi platform channel bạn viết là món nợ phải trả trên hai OS.

| Thư viện | Vai trò |
|---|---|
| `esc_pos_utils` | Sinh byte lệnh ESC/POS (text, căn lề, QR, cắt giấy) |
| `flutter_pos_printer_platform` / `blue_thermal_printer` | Truyền qua Bluetooth / USB |

**Những chỗ hay vấp:**
- **Tiếng Việt có dấu**: máy in nhiệt Trung Quốc thường không hỗ trợ codepage tiếng Việt → giải pháp bền nhất là **render bill thành ảnh bitmap rồi in ảnh**. Chậm hơn ~200ms nhưng luôn đúng dấu.
- **Khổ giấy**: K80 = 80mm ≈ 48 ký tự/dòng, K58 = 58mm ≈ 32 ký tự. Cho cấu hình, đừng hardcode.
- **Ghép nối lại**: máy in mất kết nối là chuyện thường ngày → tự động reconnect + nút "In lại" trên mỗi đơn.
- **Hàng đợi in**: đơn vào lúc máy in đang bận → xếp hàng, không bỏ.

---

## Offline — hàng đợi hành động

Mạng ở tỉnh mất 30 giây là bình thường. Không được để mất thao tác.

```
Chủ quán bấm "Xác nhận đơn"
  → ghi vào SQLite queue (drift)
  → cập nhật UI ngay (optimistic)
  → worker gửi lên server, retry backoff
  → có mạng lại: flush queue theo thứ tự
```

Mọi hành động thay đổi trạng thái đều đi qua queue này, kèm `Idempotency-Key` do client sinh. Server dedupe, nên retry an toàn.

Hiển thị rõ trạng thái đồng bộ: *"3 thao tác đang chờ gửi"* — chủ quán cần biết, đừng giấu.

---

## Báo cáo trên mobile — thiết kế khác hẳn dashboard

Đừng bê biểu đồ web xuống điện thoại. Với chủ quán ở tỉnh, thứ hiệu quả là:

**Một con số lớn + một so sánh:**
```
Hôm nay
1.240.000đ  ·  47 đơn
↑ 12% so với thứ Ba tuần trước
```

**Ba dòng, không phải ba biểu đồ:**
- Món bán chạy nhất hôm nay
- Giờ đông nhất
- Số khách quay lại

**Sổ thu chi cuối ngày** — chủ shop quan tâm cái này hơn analytics.

### ⭐ Push tổng kết cuối ngày — ROI cao nhất trong cả app

> *"Tối nay quán mình bán 47 đơn, 1.240.000đ. Món chạy nhất: bún bò đặc biệt."*

Vì sao mạnh: chủ quán **không cần mở app** vẫn thấy giá trị mỗi ngày. Vừa là báo cáo, vừa là hook kéo họ quay lại, vừa là thứ họ khoe với quán bên cạnh. Chi phí gần bằng 0.

Cân nhắc làm rất sớm — có thể trước cả màn hình báo cáo đầy đủ.

### "Ngày kinh doanh" ≠ ngày lịch

Quán ăn đêm đóng cửa 2h sáng. Đơn lúc 1h sáng, với chủ quán, là doanh thu của **tối hôm qua**.

Cắt theo nửa đêm → báo cáo luôn sai → chủ quán không tin số liệu của bạn → mất niềm tin vào toàn bộ sản phẩm.

BE có `tenants.business_day_start` (mặc định `04:00`). App chỉ hiển thị theo mốc đó, không tự tính.

---

## Công nghệ tích hợp

| Công nghệ | Why | How |
|---|---|---|
| **FCM data message** | Đánh thức app khi background | `data` payload (không phải `notification`) để tự kiểm soát hiển thị |
| **Foreground service** (native) | Giữ app sống, kêu chuông đáng tin | Platform channel → Android `Service` + persistent notification |
| **`flutter_local_notifications`** | Full-screen intent, kênh thông báo riêng, âm ALARM | Tạo channel `new_order` với importance MAX |
| **`esc_pos_utils` + printer platform** | In bill K80/K58 | Render bitmap để đúng dấu tiếng Việt |
| **`drift` (SQLite)** | Hàng đợi offline + cache đơn | Migration có version, đừng dùng shared_prefs cho dữ liệu này |
| **`workmanager`** | Retry queue khi app không chạy | Kết hợp với foreground service |
| **`device_info_plus`** | Detect OEM để hướng dẫn autostart | `Build.MANUFACTURER` → mở intent cài đặt tương ứng |
| **Crashlytics + Sentry** | App này crash = mất tiền của quán | Cả hai; Sentry cho non-fatal + breadcrumb |
| **`ds_tokens` + `ds_components`** | Dùng chung design system | **Không** import `ds_sdui` — giữ app nhẹ |

**Platform channel — giữ dưới 4:**
1. Foreground service + chuông lặp
2. Máy in (nếu package sẵn không đủ)
3. Xin miễn tối ưu pin + mở cài đặt autostart
4. *(sau)* Background location cho tài xế

---

## Cấu hình UI mức visibility

Đúng phạm vi đã chốt — chỉ ẩn/hiện, không chỉnh style:

```json
PATCH /v1/merchant/storefront/visibility
{ "hidden_blocks": ["promo_strip_2"], "hidden_categories": ["cat_hết_mùa"] }
```

UI trong app: một danh sách block với công tắc bật/tắt, kèm preview nhỏ. Không kéo thả, không color picker.

Kèm những thao tác nhanh dùng hàng tuần:
- Đổi ảnh banner khuyến mãi
- Hết món / mở lại món
- Sửa giá
- Đổi giờ mở cửa, tạm đóng quán

---

## Checklist độ tin cậy — bar để coi là "xong"

- [ ] Test trên **máy Xiaomi và Oppo thật**, không phải emulator
- [ ] Để app chạy nền **8 tiếng liên tục**, gửi đơn thử → chuông vẫn kêu
- [ ] Khởi động lại điện thoại → app tự chạy lại và vẫn nhận đơn
- [ ] Tắt Wi-Fi giữa lúc xác nhận đơn → thao tác vào queue, tự gửi khi có mạng
- [ ] Máy in tắt rồi bật → tự reconnect, in lại được
- [ ] Bill in ra **đúng dấu tiếng Việt** trên máy in thật
- [ ] Chuông nghe rõ trong môi trường ồn, kêu lặp tới khi xác nhận
- [ ] Màn hình tự kiểm tra báo đúng khi bị OEM chặn
- [ ] Push trùng không tạo hai bản ghi đơn

Bar cho merchant app không phải "trông đẹp" mà là: **để máy chạy nền 8 tiếng, gửi đơn lúc 3h sáng, chuông vẫn kêu.**
