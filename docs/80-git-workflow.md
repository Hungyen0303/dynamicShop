# 80 — Git workflow

Trả lời câu hỏi: *"sửa ở folder mobile thì folder BE có thấy không, có bị loạn không?"*

---

## Câu trả lời ngắn

**Có, cùng một repo nên `git status` sẽ hiện thay đổi của mọi folder.** Nhưng đó không phải "loạn" — đó là thứ bạn cần, và có ba công cụ để nó không phiền.

---

## Vì sao vẫn nên monorepo

Vấn đề thật không phải là git status dài. Vấn đề là **contract**.

```
contracts/openapi.yaml  →  sinh ra client Dart (mobile)
                        →  sinh ra interface Java (backend)
```

Nếu tách repo, kịch bản này xảy ra trong tuần đầu tiên:

```
Thứ 2: sửa openapi.yaml, commit ở repo backend
Thứ 3: quên sinh lại client ở repo mobile
Thứ 4: mobile gọi API với field cũ → lỗi 400 → mất nửa ngày tìm
```

Monorepo làm cho việc đó **không thể xảy ra**: một commit chứa cả contract, cả backend, cả client sinh ra. CI chạy `make verify-contracts` trên toàn bộ. Lệch là fail ngay.

Với một người làm cùng AI agent, đây là lợi ích lớn hơn nhiều so với phiền toái của git status dài.

---

## Ba công cụ chống loạn

### 1. Sparse checkout — chỉ lấy folder đang làm

Đây là thứ giải quyết trực tiếp mối lo của bạn. Máy bạn **chỉ có** folder đang làm việc, các folder khác không xuất hiện.

```bash
# clone nhưng chưa lấy file nào
git clone --filter=blob:none --sparse <repo> dynamicshop
cd dynamicshop

# chỉ lấy phần backend
git sparse-checkout set backend contracts docs

# khi chuyển sang mobile
git sparse-checkout set apps packages contracts docs
```

Kết quả: làm backend thì `ls` chỉ thấy `backend/ contracts/ docs/`. `git status` chỉ hiện thay đổi trong đó. Không thấy mobile.

Đổi qua lại chỉ mất vài giây và không mất dữ liệu — file vẫn nằm trong git, chỉ là không được trải ra đĩa.

### 2. Git worktree — làm hai phần cùng lúc, hai thư mục riêng

Khi cần mở backend và mobile song song (ví dụ đang debug API):

```bash
git worktree add ../ds-backend  develop
git worktree add ../ds-mobile   develop
```

Hai thư mục riêng biệt trên đĩa, chung một git history. Mở hai cửa sổ IDE, mỗi cái một phần, `git status` của mỗi thư mục độc lập.

### 3. Commit scope — lịch sử đọc được

```
be:  thêm idempotency cho tạo đơn
mo:  render block product_grid
web: trang 6 chỉ số
ct:  thêm field business_day_start vào openapi
doc: cập nhật stage
```

`git log --oneline | grep '^be:'` cho ra lịch sử riêng của backend. Sạch như repo tách rời, nhưng vẫn chung một nguồn sự thật.

---

## Quy tắc commit

**Một commit = một mục đích, không phải một folder.**

✅ Đúng — thay đổi contract lan qua nhiều folder trong **một** commit:
```
ct: thêm field note vào đơn hàng

contracts/openapi.yaml       | sửa
backend/.../OrderDto.java    | sinh lại
packages/ds_api/.../order.dart | sinh lại
```
Đây chính là lý do chọn monorepo. Tách ra thành 3 commit ở 3 repo là tạo ra cửa sổ để lệch.

❌ Sai — gộp hai việc không liên quan:
```
be: sửa idempotency + mo: đổi màu nút + doc: sửa typo
```

**Trước khi commit, luôn `git status` và kiểm tra không có file lạ.** Với sparse checkout thì rủi ro này gần như bằng 0.

---

## Nhánh

```
main      production
develop   tích hợp
feat/*    tính năng
fix/*     sửa lỗi
```

Giai đoạn Stage 0 làm một mình: có thể chỉ dùng `main` + `feat/*`. Thêm `develop` khi bắt đầu deploy (Stage 1).

---

## Nếu vẫn muốn tách repo

Được, nhưng phải trả một cái giá bắt buộc: **`contracts/` thành repo riêng, và cả ba repo kia nhúng nó vào làm git submodule** — kèm CI kiểm tra mọi repo đang trỏ cùng một commit của contracts.

```
ds-contracts   (repo riêng)
ds-backend     (submodule → ds-contracts)
ds-mobile      (submodule → ds-contracts)
ds-web         (submodule → ds-contracts)
```

Không làm bước này thì contract sẽ drift trong vòng một tháng, và bạn mất đúng thứ mà toàn bộ thiết kế docs này dựng lên để bảo vệ.

Git submodule khá phiền khi làm một mình. **Khuyến nghị: monorepo + sparse checkout.** Thử một tuần, nếu thấy vẫn loạn thì chuyển sang multi-repo — chuyển từ mono sang multi dễ hơn chiều ngược lại.

---

## Không commit

Đã có trong `.gitignore`, nhưng nhắc lại vì đắt nếu sai:

```
.env, .env.*
google-services.json, GoogleService-Info.plist
*.jks, *.keystore, *.p8, *.p12
service-account*.json
application-local.yml
**/generated/     ← sinh lại được, đừng commit
```
