.PHONY: help up down db test stage0-check generate verify-contracts lint

help: ## Danh sách lệnh
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS=":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ---------- STAGE 0 : local ----------

up: ## Chạy Postgres (Docker)
	cd infra/docker && docker compose up -d

down: ## Dừng Postgres
	cd infra/docker && docker compose down

db: ## Kiểm tra 2 role app_user / app_admin đã tạo chưa
	cd infra/docker && docker compose exec postgres psql -U postgres -d dynamicshop -c "\\du"

test: ## Test backend (gồm test cô lập tenant)
	cd backend && ./gradlew test

stage0-check: ## Nhắc 8 bước flow phải chạy tay
	@echo "Bar hoàn thành Stage 0 — chạy tay 8 bước trong docs/70-stages.md:"
	@echo "  1. customer_app mở mock shop A, storefront đúng theme+menu"
	@echo "  3. đổi sang shop B -> theme & menu KHAC HAN"
	@echo "  4-5. đặt đơn -> DB có đơn + order_events có PENDING"
	@echo "  6. xác nhận đơn qua curl -> order_events thêm dòng"
	@echo "  7. gửi lại cùng Idempotency-Key -> KHÔNG tạo đơn thứ hai"
	@echo "  8. query với tenant B -> KHÔNG thấy đơn của tenant A"

# ---------- STAGE 1+ : chưa dựng ----------

generate: ## (Stage 1+) sinh code từ contracts/
	@echo "Stage 1+. Chưa chọn công cụ sinh code — xem contracts/README.md, HỎI NGƯỜI trước."
	@exit 1

verify-contracts: ## (Stage 1+) fail nếu code lệch contracts/
	@echo "Stage 1+. Dựng các guard trong docs/50-qa.md muc 4."
	@exit 1

lint: ## (Stage 1+) lint + check hardcode style
	@echo "Stage 1+. Ở Stage 0 chỉ cần: cd backend && ./gradlew test"
	@exit 1
