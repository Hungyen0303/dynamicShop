# DynamicShop — Chiến lược tổng hợp

*Tài liệu tham chiếu — tháng 7/2026*

---

## 1. Định vị (đã tinh chỉnh)

**Trước:** "App mua hàng online cho shop vừa và nhỏ ở tỉnh."

**Sau:** *Hạ tầng bán hàng cho đơn khách quen của shop — shop tự có khách, không lý do gì phải chia 25% cho sàn.*

Điểm khác biệt cốt lõi: **không cạnh tranh traffic với ShopeeFood/GrabFood.** Sàn bán traffic và lấy 20–30%. DynamicShop bán công cụ để shop giữ trọn đơn mình đã tự có — Facebook inbox, Zalo, khách gọi điện, khách quen.

**Ba lời hứa với chủ shop:**

| Lời hứa | Vì sao mạnh |
|---|---|
| Tiền về thẳng tài khoản shop | Sàn giữ tiền 7–14 ngày. Shop nhỏ sống bằng dòng tiền ngày. |
| Không mất phí phần trăm mỗi đơn | Phí cố định vài trăm nghìn/tháng thay vì 25%/đơn |
| Thương hiệu shop lên app trong 3h | Cảm giác "có app riêng" mà không tốn gì |

**Lưu ý pháp lý — nguyên tắc bất di bất dịch:** tiền đi thẳng shop qua VietQR động, **không bao giờ chảy qua ví của DynamicShop**. Giữ nguyên tắc này thì không cần giấy phép trung gian thanh toán. Vi phạm nó một lần là mở ra cả một tầng rủi ro pháp lý.

---

## 2. Kiến trúc sản phẩm — mô hình "Dynamic App"

Ba tầng, đi từ rẻ đến đắt:

```
Tầng 1 — Web / PWA  (miễn phí, tức thì)
   Link riêng cho shop, khách bấm là đặt, không cài gì

Tầng 2 — Dynamic App  (app tổng, sub-app trong 3h)
   Một app duy nhất trên store, chứa storefront của mọi shop
   Khách cài 1 lần, dùng cho mọi quán
   Shop có "app của mình" trong 3h — dùng thử, cảm nhận thương hiệu

Tầng 3 — App riêng  (gói cao cấp, upsell)
   Native app thương hiệu riêng, publish dưới developer account của shop
```

### Vì sao kiến trúc này đúng

- **Giải quyết rào cản khách:** khách ở tỉnh không cài app cho một quán bún. Nhưng cài một app dùng được cho 40 quán thì có.
- **Né Apple Guideline 4.3:** Apple reject hàng loạt app template giống nhau từ một developer account. Một app tổng thì không dính.
- **"3h" trở thành lời hứa thật:** không cần store review, chỉ cần dựng storefront.
- **Tạo ra moat:** khi 40 quán một thị xã cùng nằm trong app, khách bắt đầu mở app để *tìm đồ ăn* chứ không chỉ để đặt ở quán quen. Bạn có traffic của riêng mình — thứ mô hình SaaS thuần không bao giờ có.

### Lằn ranh phải giữ: hạ tầng, không phải sàn

Đây là cái bẫy lớn nhất của mô hình app tổng. Nếu bạn bắt đầu điều phối traffic, bạn thành một sàn nhỏ và sẽ thua sàn lớn.

| ✅ Làm | ❌ Không làm |
|---|---|
| Deep link: khách bấm link Zalo/FB của shop → mở thẳng storefront shop | Trang chủ xếp hạng quán, "quán hot", "quán nổi bật" |
| Trong storefront hiển thị 100% brand shop (logo, màu, banner) | Ép hiển thị brand DynamicShop nổi bật |
| Danh sách quán chỉ là tiện ích phụ, sắp theo khoảng cách / đang mở | Thuật toán phân phối đơn do DynamicShop quyết |
| Traffic phát sinh tự nhiên → tặng shop miễn phí | Bán vị trí, đấu giá hiển thị |

Traffic là **bonus bạn tặng**, không phải thứ bạn bán. Có thể mở kênh quảng cáo tự nguyện khi đã lớn — nhưng không phải trong 2 năm đầu.

### Ghi chú kỹ thuật cho gói app riêng

Cách chuẩn để qua Apple: **publish dưới developer account của chính shop**, DynamicShop làm hộ thủ tục. Đừng gom về account của mình.

---

## 3. Tính năng — ưu tiên theo giai đoạn

### Bắt buộc có trước khi bán (MVP)

- Storefront: menu, giá, ảnh, giờ mở cửa
- Đặt hàng + VietQR động (tiền về thẳng shop)
- Quản lý đơn cho chủ shop (nhận / xác nhận / hoàn thành)
- **In bill máy nhiệt K80** — F&B gần như bắt buộc, thiếu là mất đơn
- Deep link từ Zalo/Facebook vào thẳng storefront
- Import menu nhanh (để dựng 20 storefront/tối phục vụ đi bộ bán hàng)

### Giai đoạn 2 — giữ chân

- **Đơn tại quán** (POS nhẹ) — không chỉ đơn online
- **Sổ thu chi cuối ngày** — chủ shop quan tâm cái này hơn analytics
- **Thông báo trong app** cho khách *(đã chọn thay cho Zalo ZNS)*
  → lưu ý: tỉ lệ khách bật notification thường chỉ 40–60%. Giữ Zalo làm kênh dự phòng cho khách đã tắt noti, đừng bỏ hẳn.
- Tích hợp **Ahamove / Grab Express / Be** — hầu hết shop tỉnh không có shipper riêng. "Chủ động về tài xế" phải bao gồm cả gọi ship ngoài, không chỉ quản lý shipper của shop.

### Giai đoạn 3 — CRM khách quen *(đã chốt làm)*

Đây là tầng tạo giá trị cao nhất và cũng là moat mềm.

**Nguyên tắc: đóng gói thành hành động, không phải biểu đồ.** Với 50–500 khách/shop thì "phân tích hành vi tiêu dùng" là oversell — mẫu quá nhỏ, và chủ shop cũng không biết đọc dashboard.

Thay vào đó:

- *"12 khách quen 30 ngày chưa quay lại → gửi voucher?"* (một nút bấm)
- *"Món này bán chạy nhất tối thứ 6 → đẩy combo"*
- *"Khách A đã mua 8 lần → gợi ý tặng món"*

Mỗi insight phải kèm **một hành động bấm được**. Không có nút bấm thì đừng hiển thị.

### Giai đoạn 4 — mở sang shop quần áo

Tồn kho theo size/màu, đổi trả, ảnh sản phẩm nhiều góc. Chưa động vào cho tới khi F&B vững.

---

## 4. Giá

Thay cho "miễn phí tới khi có doanh thu" (không verify được, hút shop không nghiêm túc, và chuyển free→paid là điểm churn lớn nhất):

| Gói | Giá | Nội dung |
|---|---|---|
| Dùng thử | Free 30 ngày | Web + sub-app trong Dynamic App, ≤50 đơn |
| Cơ bản | 199–299k/tháng | Không giới hạn đơn, QR thanh toán, quản lý đơn, in bill |
| Pro | 499–699k/tháng | CRM khách quen, khuyến mãi, nhiều chi nhánh, quản lý tài xế |
| App riêng | 3–5tr setup + phí tháng | Native app thương hiệu riêng, publish dưới account shop |

**Phương án thay thế đáng cân nhắc:** phí theo đơn cực thấp, 500–1.000đ/đơn. Về mặt tâm lý nó cực mạnh khi đặt cạnh "25% mỗi đơn của sàn" — nhưng cần GMV đủ lớn mới sống được. Theo dõi chỉ số GMV (mục 6) để biết khi nào chuyển được.

---

## 5. Go-to-market

**Phạm vi giai đoạn đầu: 1 tỉnh, 1 ngành (F&B), 1 cụm địa lý.**
Ví dụ: các quán ăn quanh một trường đại học.
Mục tiêu 3 tháng: **10 shop có đơn thật mỗi tuần** — không phải 100 shop đăng ký.

### Kênh 1 — Đi bộ trực tiếp

Chủ quán ở tỉnh không search Google tìm phần mềm, không đọc email marketing. Họ mua vì có người thật đứng trước mặt.

**Cách làm — đừng bán, hãy đưa hàng đã làm sẵn:**

1. Tối hôm trước: chọn 20 quán trong cụm 500m, vào Facebook họ chụp menu
2. Dựng sẵn 20 storefront (2–3 phút/quán nếu tool import tốt)
3. Sáng hôm sau: *"Em làm sẵn trang bán hàng cho quán mình rồi, anh xem thử 30 giây"* → đưa điện thoại có sẵn menu, giá, hình của **chính quán họ**

Đây là lúc "3h" thành vũ khí thật: bạn không hứa, bạn đã làm xong rồi.

**Phễu thực tế:** 100 quán tiếp cận → ~30 chịu nghe → ~10 dùng thử → ~3–4 trả tiền. Ghi lại từng con số, đừng đi bằng cảm giác.

### Kênh 2 — Hội nhóm Facebook địa phương

Các group *"Ăn vặt [tỉnh]"*, *"Chợ [tỉnh]"*, *"Review đồ ăn [tỉnh]"* có 50k–200k thành viên và **đó là nơi khách thật sự tìm đồ ăn** — không phải Google, không phải app.

- **Sai:** đăng quảng cáo DynamicShop. Không ai quan tâm phần mềm.
- **Đúng:** đăng hộ shop khách hàng — ảnh món ăn + link đặt hàng.

Kết quả kép: shop hiện tại có đơn thật nên không bỏ bạn (đây là *retention*, không phải marketing), và shop khác nhìn thấy rồi tự hỏi *"sao quán đó có link đặt hàng bấm được vậy?"* → inbound miễn phí.

**Kèm theo:** trả tiền admin group. Ở tỉnh chỉ vài trăm nghìn đến vài triệu/tháng cho quyền pin bài — rẻ hơn Facebook Ads nhiều lần và nhắm đúng người hơn.

### Kênh 3 — Đại lý địa phương ăn hoa hồng

Kênh mạnh nhất, rẻ nhất, khó copy nhất.

**Ai nên tuyển, xếp theo độ hiệu quả:**

1. **Người giao nguyên liệu** — giao đá, giao thịt, giao nguyên liệu trà sữa. Họ bước vào bếp 30–50 quán *mỗi ngày* và chủ quán tin họ. Mỏ vàng ít ai để ý.
2. **Nhân viên bán máy in bill / POS / thiết bị bếp** — đã bán thứ khác cho cùng tệp khách
3. **Kế toán làm thuê theo giờ** cho nhiều quán nhỏ
4. Sinh viên năm 3–4 — dễ tuyển nhất, chất lượng thấp nhất

**Cơ chế hoa hồng:**

- 30–50% doanh thu **tháng đầu** của shop mang về
- **10–15% recurring trong 12 tháng** ← phần quan trọng nhất

Recurring biến CTV thành người *chăm sóc* shop thay bạn: shop gặp trục trặc thì CTV chạy tới giúp, vì họ mất tiền nếu shop bỏ. Bạn có sales và customer success mà không phải thuê ai.

**Chống gian lận:** chỉ trả hoa hồng khi shop đạt **≥20 đơn thật trong 30 ngày đầu** — không trả theo lượt đăng ký. Trả theo đăng ký thì ngập shop rác trong 2 tuần.

---

## 6. Chỉ số sống còn

Không đếm số shop đăng ký. Đó là chỉ số phù phiếm khiến bạn tưởng mình có product-market fit trong khi không.

| # | Chỉ số | Vì sao | Ngưỡng |
|---|---|---|---|
| 1 | **% shop có ≥1 đơn ở tuần thứ 4** | Tuần 1–2 shop còn tò mò, chưa phản ánh gì. Tuần 4 mới thật. | >50%. Dưới 30% → sửa sản phẩm, đừng tuyển sales |
| 2 | **Số đơn/shop/tuần** | Chỉ số dự báo churn tốt nhất. 20 đơn/tuần gần như không bao giờ bỏ; 2 đơn/tuần sẽ bỏ trong 2 tháng dù họ nói gì. | Tìm ra **ngưỡng ma thuật** của riêng bạn |
| 3 | **Retention tháng 3** | Tháng 1 trả vì tò mò/nể. Tháng 3 mới là quyết định thật. | <60% → **tuyệt đối không scale** |
| 4 | **GMV** | Kiểm chứng mô hình phí/đơn, cơ sở để tăng giá, dùng khi gọi vốn | Theo dõi xu hướng |
| 5 | **CAC vs LTV** | Giá 250k/tháng × 12 tháng ≈ LTV 3tr → CAC phải <1tr. **Quyết định bạn được trả CTV bao nhiêu.** | CAC < 1/3 LTV |
| 6 | **Time-to-first-order** | Quá 48h thì xác suất shop chết rất cao. Kiểm soát được ngay bằng quy trình onboarding. | <24h |

**Về chỉ số #2 — thứ giá trị nhất trong danh sách này:** khi bạn phát hiện được *"shop vượt 15 đơn/tuần thì retention 90%, dưới thì 40%"*, toàn bộ roadmap sản phẩm có một mục tiêu duy nhất và rõ ràng: **đẩy shop qua ngưỡng đó.** Không còn phải đoán nên làm tính năng gì.

---

## 7. Moat — xây từ đâu

Phần mềm này copy được trong 2 tháng. Rào cản thật nằm ở bốn thứ, và cả bốn đều phải được thiết kế để dày lên theo thời gian:

1. **Dữ liệu khách quen bị khoá trong hệ thống** — shop chuyển đi = mất lịch sử khách. Đây là lý do CRM (mục 3, giai đoạn 3) quan trọng hơn nó có vẻ.
2. **Thói quen vận hành hàng ngày** — khi shop in bill, chốt sổ, xem đơn bằng DynamicShop mỗi ngày thì việc đổi là đau đớn.
3. **Mạng lưới CTV địa phương** — đối thủ ở Sài Gòn không copy được quan hệ với người giao đá ở Rạch Giá.
4. **Mật độ trong app tổng** — khi đủ quán một khu vực, app bắt đầu tự có traffic.

---

## 8. Rủi ro cần theo dõi

| Rủi ro | Dấu hiệu sớm | Cách giảm |
|---|---|---|
| Shop churn cao (quán đóng cửa nhiều) | Retention tháng 3 <60% | Nhắm quán đã hoạt động >1 năm |
| Chủ shop lười đổi thói quen | Time-to-first-order kéo dài | Onboarding tận tay, dựng sẵn menu |
| Trượt dần thành sàn | Bắt đầu có yêu cầu "cho quán tôi lên đầu" | Giữ lằn ranh ở mục 2 |
| CTV mang shop rác | Đăng ký tăng, đơn/shop giảm | Trả hoa hồng theo đơn thật, không theo đăng ký |
| Bị cám dỗ giữ tiền hộ | — | Nguyên tắc bất di bất dịch ở mục 1 |

---

## 9. Kế hoạch 90 ngày

**Tuần 1–4 — Dựng nền**
- MVP: storefront + VietQR + quản lý đơn + in bill K80 + deep link
- Tool import menu nhanh
- Lập file theo dõi 6 chỉ số ngay từ shop đầu tiên
- Chọn cụm địa lý mục tiêu, khảo sát 50 quán

**Tuần 5–8 — 10 shop đầu tiên**
- Đi bộ trực tiếp, tự bán, không thuê ai
- Toàn bộ miễn phí, đổi lại yêu cầu shop dùng thật
- Mục tiêu: 10 shop có đơn thật, time-to-first-order <24h
- Ghi lại mọi lý do từ chối và mọi lời phàn nàn

**Tuần 9–12 — Kiểm chứng và mở**
- Đưa Dynamic App (app tổng) lên store
- Bắt đầu thu tiền từ nhóm shop đầu — đây là bài kiểm tra thật
- Đăng bài hộ shop trong group Facebook, đo đơn phát sinh
- Tuyển 2–3 CTV thử nghiệm, chạy cơ chế hoa hồng
- **Cổng quyết định:** retention tháng 3 ≥60% và đơn/shop/tuần có xu hướng tăng → mới scale. Không đạt → quay lại sửa sản phẩm.

---

## 10. Nguyên tắc rút gọn

1. Không giữ tiền của shop. Bao giờ cũng vậy.
2. Hạ tầng, không phải sàn. Không bán vị trí hiển thị.
3. Đếm shop *có đơn*, không đếm shop *đăng ký*.
4. Không scale trước khi retention tháng 3 vượt 60%.
5. Mỗi insight phải kèm một nút bấm được.
6. Hoa hồng trả theo đơn thật, không theo lượt đăng ký.
7. F&B trước. Xong hẳn rồi mới sang ngành khác.
