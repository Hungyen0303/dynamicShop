#!/usr/bin/env bash
# Chặn hardcode style trong package dùng chung.
# Không có bước này, sau 3 tháng ~30% widget sẽ lén hardcode màu
# và multi-style hỏng IM LẶNG — chỉ vài shop bị, không ai biết.
set -euo pipefail

TARGETS="packages/ds_blocks/lib packages/ds_components/lib"
PATTERN="Colors\.|Color\(0x|BorderRadius\.circular\([0-9]"

for d in $TARGETS; do
  [ -d "$d" ] || continue
  if grep -rnE "$PATTERN" "$d"; then
    echo "❌ Hardcoded style trong package dùng chung: $d"
    echo "   Dùng token từ ds_tokens. Xem docs/60-design.md"
    exit 1
  fi
done
echo "✅ Không có hardcode style"
