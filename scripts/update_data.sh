#!/bin/bash
# 卡厄思梦境(CZN) Wiki 数据更新脚本
# 用法: ./scripts/update_data.sh
#
# 从 GameKee 抓取最新活动数据并更新到 assets 目录

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DATA_DIR="$PROJECT_DIR/app/src/main/assets/data"

echo "===== CZN Wiki 数据更新 ====="
echo "数据目录: $DATA_DIR"
echo ""

# 备份现有数据
BACKUP_DIR="$DATA_DIR/backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
if [ -f "$DATA_DIR/events.json" ]; then
    cp "$DATA_DIR/events.json" "$BACKUP_DIR/"
    echo "[备份] events.json → $BACKUP_DIR/"
fi
if [ -f "$DATA_DIR/banners.json" ]; then
    cp "$DATA_DIR/banners.json" "$BACKUP_DIR/"
    echo "[备份] banners.json → $BACKUP_DIR/"
fi

# 运行抓取脚本
echo ""
echo "[执行] 运行抓取脚本..."
python3 "$SCRIPT_DIR/gamekee_scraper.py" --output "$DATA_DIR"

# 验证输出
echo ""
echo "[验证] 输出文件:"
if [ -f "$DATA_DIR/events.json" ]; then
    EVENT_COUNT=$(python3 -c "import json; print(len(json.load(open('$DATA_DIR/events.json'))))")
    echo "  events.json: ${EVENT_COUNT} 条活动"
fi
if [ -f "$DATA_DIR/banners.json" ]; then
    BANNER_COUNT=$(python3 -c "import json; print(len(json.load(open('$DATA_DIR/banners.json'))))")
    echo "  banners.json: ${BANNER_COUNT} 条卡池"
fi

echo ""
echo "===== 更新完成 ====="
echo "如需回滚，备份在: $BACKUP_DIR"