# Bridge Next.js ↔ Flutter studio

Message types định nghĩa ở `contracts/studio-bridge.schema.json`. Thêm type mới ⇒ sửa contract trước.

```js
// Next.js → Flutter (debounce ~200ms)
iframe.contentWindow.postMessage(
  { type: 'layout_update', payload: draftLayout }, TRUSTED_ORIGIN);

// Flutter → Next.js
window.parent.postMessage(
  { type: 'block_selected', id: 'grid_1' }, TRUSTED_ORIGIN);
```

## Bắt buộc
- **Luôn kiểm tra `event.origin`.** Không bao giờ `'*'`.
- Flutter không giữ state, không gọi API. Next.js giữ toàn bộ state, form, undo/redo, lưu nháp.
- Preview dùng **data thật của shop**, không dùng mock — để lộ ra lỗi kiểu "tên món quá dài bị tràn" ngay trong studio.

## Nếu preview lệch app thật
Nghĩa là ai đó đã dựng lại block ở phía web. Đó là lỗi kiến trúc — sửa trong `packages/ds_blocks`, không sửa trong `studio_web`.
