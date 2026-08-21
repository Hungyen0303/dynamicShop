# CLAUDE.md

Repo này dùng **`AGENTS.md`** làm file hướng dẫn gốc cho mọi AI coding agent.

👉 **Đọc [`AGENTS.md`](./AGENTS.md) trước khi làm bất cứ việc gì.**

Không thêm luật vào file này. Mọi thay đổi về quy tắc làm việc đều sửa trong `AGENTS.md`, để agent nào cũng nhận được cùng một chỉ dẫn.

---

## Các file trỏ về cùng một nguồn

| Agent | File | Nội dung |
|---|---|---|
| Claude Code | `CLAUDE.md` | file này — trỏ về `AGENTS.md` |
| Cursor | `.cursorrules` | trỏ về `AGENTS.md` |
| GitHub Copilot | `.github/copilot-instructions.md` | trỏ về `AGENTS.md` |
| Codex / khác | `AGENTS.md` | đọc trực tiếp |

Nếu dùng hệ thống hỗ trợ symlink, thay các file trên bằng symlink tới `AGENTS.md` để không bao giờ lệch nhau:

```bash
ln -sf AGENTS.md CLAUDE.md
ln -sf ../AGENTS.md .github/copilot-instructions.md
```
