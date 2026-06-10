#!/usr/bin/env python3
"""
卡厄思梦境(CZN) GameKee Wiki 动态数据抓取与解析脚本

数据源:
- 活动: https://www.gamekee.com/czn/huodong/104
- 卡池: https://www.gamekee.com/czn/kachi/104

两种工作模式:

  模式1 - HTTP 直接抓取 (对 JS 渲染页面无效):
      python gamekee_scraper.py --output ./data/

  模式2 - 从预抓取的 HTML 文件解析 (推荐):
      python gamekee_scraper.py --huodong-html huodong.html --kachi-html kachi.html --output ./data/

  模式3 - 从 stdin 传入 JSON 数据 (用于与 AI Agent 协作):
      echo '{"activities":[...], "banners":[...]}' | python gamekee_scraper.py --from-stdin --output ./data/

输出 JSON 格式与 app/src/main/assets/data/ 下的 Entity 结构一致。
"""

import argparse
import json
import os
import re
import sys
from datetime import datetime, timedelta
from typing import Optional
from urllib.request import Request, urlopen
from urllib.error import URLError

# ===== 配置 =====
ACTIVITY_URL = "https://www.gamekee.com/czn/huodong/104"
BANNER_URL = "https://www.gamekee.com/czn/kachi/104"
USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

# ===== 工具函数 =====

def http_get(url: str) -> Optional[str]:
    """简单的 HTTP GET，不适用于 JS 渲染页面。"""
    req = Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urlopen(req, timeout=15) as resp:
            return resp.read().decode("utf-8", errors="replace")
    except URLError as e:
        print(f"[WARN] HTTP GET 失败: {url}, {e}", file=sys.stderr)
        return None


def compute_end_date(days: int, base_date: Optional[datetime] = None) -> str:
    """计算结束日期: base_date + days"""
    if base_date is None:
        base_date = datetime.now()
    end = base_date + timedelta(days=days)
    return end.strftime("%Y-%m-%d")


# ===== 活动解析 (从 GameKee huodong 页面渲染后文本) =====

def _clean_name(name: str) -> str:
    """清理活动名称：去除前导数字/日期碎片，以及多余的描述后缀。"""
    name = name.strip()
    # 去除纯日期碎片前导（如 "04" 但保留 "9号" "3月" 等有意义数字）
    name = re.sub(r'^(?:0[1-9]|[12]\d|3[01])(?=[\u4e00-\u9fff（(])', '', name)
    # 去除 GameKee 描述后缀
    name = re.sub(r'\s+新(?:功能|卡厄思|进干员|版本|角色).*$', '', name)
    name = re.sub(r'\s+(?:21日长签到活动|7日签到|释放政策活动).*$', '', name)
    return name.strip()


def parse_activities_from_text(text: str) -> list[dict]:
    """
    解析活动数据。
    
    GameKee 活动页面的文本格式:
    活动名 进行中/已结束 N天后结束 或 YY.MM.DD-YY.MM.DD
    """
    activities = []
    now = datetime.now()
    activity_id = 0
    
    # 先在活动之间插入换行符，解决无分隔符导致名称/日期串位问题
    # 策略：在每个 \"结束\" 或 \"已结束\" 后面插入 \\n
    text = re.sub(r'(天后结束|已结束)', r'\1\n', text)
    # 此外，在日期终点（YY.MM.DD 后紧跟中文）插入换行，防止日期泄漏到下一个活动名
    text = re.sub(r'(\d{2})\.(\d{2})\.(\d{2})([\u4e00-\u9fff])', r'\1.\2.\3\n\4', text)
    
    # 匹配模式 1: "活动名 进行中 N天后结束"
    # 名称后可能跟非中文描述文字，用 .*? 跳过直到匹配 已结束/天后结束
    pattern1 = re.compile(
        r'([\u4e00-\u9fff\w·「」\-（）()，、。！？；：""''…—～【】《》]+).*?(进行中)\s+(\d+)\s*天后结束',
        re.UNICODE
    )
    # 匹配模式 2: "活动名 已结束 YY.MM.DD-YY.MM.DD"
    pattern2 = re.compile(
        r'([\u4e00-\u9fff\w·「」\-（）()，、。！？；：""''…—～【】《》]+).*?(已结束)\s+(\d{2})\.(\d{2})\.(\d{2})\s*[-~]\s*(\d{2})\.(\d{2})\.(\d{2})',
        re.UNICODE
    )
    
    seen_names = set()
    
    for m in pattern1.finditer(text):
        name = _clean_name(m.group(1))
        days = int(m.group(3))
        
        if name in seen_names or not name:
            continue
        seen_names.add(name)
        
        activity_id += 1
        activities.append({
            "id": activity_id,
            "name": name,
            "description": "",
            "endDate": compute_end_date(days, now),
            "url": ACTIVITY_URL,
            "server": "国际服"
        })
    
    for m in pattern2.finditer(text):
        name = _clean_name(m.group(1))
        # FIX: groups: 3=startYY, 4=startMM, 5=startDD, 6=endYY, 7=endMM, 8=endDD
        end_date = f"20{m.group(6)}-{m.group(7)}-{m.group(8)}"
        
        if name in seen_names or not name:
            continue
        seen_names.add(name)
        
        activity_id += 1
        activities.append({
            "id": activity_id,
            "name": name,
            "description": "",
            "endDate": end_date,
            "url": ACTIVITY_URL,
            "server": "国际服"
        })
    
    return activities


# ===== 卡池解析 (从 kachi 页面渲染后文本 + web_search 补充) =====

def parse_banners_from_text(text: str) -> list[dict]:
    """
    从 kachi 页面文本解析卡池数据。
    
    页面文本格式:
    角色名(1-3字) 进行中 还剩下 N 天 HH:MM:SS
    或 角色名 已结束 YY.MM.DD-YY.MM.DD
    """
    banners = []
    now = datetime.now()
    banner_id = 0
    
    # 匹配进行中卡池: "角色名 进行中 还剩下 N 天"
    pattern_current = re.compile(
        r'([\u4e00-\u9fff\w·]+)\s+(进行中)\s+还剩下\s+(\d+)\s+天',
        re.UNICODE
    )
    
    # 匹配已结束卡池: "角色名 已结束 YY.MM.DD-YY.MM.DD" 
    pattern_ended = re.compile(
        r'([\u4e00-\u9fff\w·]+)\s+(已结束)\s+(\d{2})\.(\d{2})\.(\d{2})\s*[-~]\s*(\d{2})\.(\d{2})\.(\d{2})',
        re.UNICODE
    )
    
    # 匹配即将开始卡池
    pattern_upcoming = re.compile(
        r'([\u4e00-\u9fff\w·]+)\s+(即将开始)',
        re.UNICODE
    )
    
    seen_names = set()
    
    for m in pattern_current.finditer(text):
        name = m.group(1).strip()
        days = int(m.group(3))
        
        if name in seen_names:
            continue
        seen_names.add(name)
        
        banner_id += 1
        banners.append({
            "id": banner_id,
            "name": name,
            "stars": 5,
            "element": "",
            "className": "",
            "type": "current",
            "startDate": "",
            "endDate": compute_end_date(days, now),
            "url": BANNER_URL,
            "server": "国际服"
        })
    
    for m in pattern_ended.finditer(text):
        name = m.group(1).strip()
        start = f"20{m.group(3)}-{m.group(4)}-{m.group(5)}"
        end = f"20{m.group(6)}-{m.group(7)}-{m.group(8)}"
        
        if name in seen_names:
            continue
        seen_names.add(name)
        
        banner_id += 1
        banners.append({
            "id": banner_id,
            "name": name,
            "stars": 5,
            "element": "",
            "className": "",
            "type": "history",
            "startDate": start,
            "endDate": end,
            "url": BANNER_URL,
            "server": "国际服"
        })
    
    for m in pattern_upcoming.finditer(text):
        name = m.group(1).strip()
        if name in seen_names:
            continue
        seen_names.add(name)
        
        banner_id += 1
        banners.append({
            "id": banner_id,
            "name": name,
            "stars": 5,
            "element": "",
            "className": "",
            "type": "upcoming",
            "startDate": "",
            "endDate": "",
            "url": BANNER_URL,
            "server": "国际服"
        })
    
    return banners


# ===== 从搜索摘要解析卡池 (web_search 兜底) =====

def parse_banners_from_search_snippets(snippets: list[str]) -> list[dict]:
    """
    从 web_search 结果摘要中解析卡池信息。
    
    搜索摘要通常包含:
    "XXXX卡池上线 5★角色[XXX]登场 卡池开放时间: XX月XX日–XX月XX日"
    """
    banners = []
    banner_id = 0
    seen = set()
    now = datetime.now()
    
    # 模式: "5★角色[名称]" 或 "5星" 搭配时间范围
    pattern = re.compile(
        r'[5５][★星]\s*角色\s*[\[【]?\s*([\u4e00-\u9fff\w·]+)\s*[\]】]?.*?'
        r'(\d{1,2})\s*月\s*(\d{1,2})\s*日\s*[–~\-至]\s*(\d{1,2})\s*月\s*(\d{1,2})\s*日',
        re.UNICODE
    )
    
    # 也匹配 "XX角色卡池"
    pattern2 = re.compile(
        r'([\u4e00-\u9fff\w·]{2,4})\s*(?:角色)?卡池.*?'
        r'(\d{1,2})\s*月\s*(\d{1,2})\s*日\s*[–~\-至]\s*(\d{1,2})\s*月\s*(\d{1,2})\s*日',
        re.UNICODE
    )
    
    for snippet in snippets:
        for m in (list(pattern.finditer(snippet)) + list(pattern2.finditer(snippet))):
            name = m.group(1).strip()
            if name in seen or len(name) < 2:
                continue
            
            try:
                start_month = int(m.group(2))
                start_day = int(m.group(3))
                end_month = int(m.group(4))
                end_day = int(m.group(5))
            except (IndexError, ValueError):
                continue
            
            start_year = now.year
            end_year = now.year
            if end_month < start_month:
                end_year += 1
            
            start_date = f"{start_year}-{start_month:02d}-{start_day:02d}"
            end_date = f"{end_year}-{end_month:02d}-{end_day:02d}"
            
            # 判断类型
            try:
                end_dt = datetime.strptime(end_date, "%Y-%m-%d")
                banner_type = "current" if end_dt >= now else "history"
            except ValueError:
                banner_type = "history"
            
            seen.add(name)
            banner_id += 1
            banners.append({
                "id": banner_id,
                "name": name,
                "stars": 5,
                "element": "",
                "className": "",
                "type": banner_type,
                "startDate": start_date,
                "endDate": end_date,
                "url": BANNER_URL,
                "server": "国际服"
            })
    
    return banners


# ===== 格式验证 =====

def validate_and_format_banners(banners: list[dict]) -> list[dict]:
    """验证卡池数据完整性，补充缺失字段默认值"""
    required = ["id", "name", "type", "endDate"]
    for b in banners:
        for k in required:
            if k not in b:
                b[k] = ""
        b.setdefault("stars", 5)
        b.setdefault("element", "")
        b.setdefault("className", "")
        b.setdefault("startDate", "")
        b.setdefault("url", BANNER_URL)
        b.setdefault("server", "国际服")
    return banners


def validate_and_format_activities(activities: list[dict]) -> list[dict]:
    """验证活动数据完整性"""
    required = ["id", "name", "endDate"]
    for a in activities:
        for k in required:
            if k not in a:
                a[k] = ""
        a.setdefault("description", "")
        a.setdefault("url", ACTIVITY_URL)
        a.setdefault("server", "国际服")
    return activities


# ===== 写入 =====

def write_json(data: list[dict], path: str):
    """写入 JSON 文件，同时写入一份带时间戳的备份。"""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"[DONE] {path} ({len(data)} 条)", file=sys.stderr)


# ===== 主流程 =====

def main():
    parser = argparse.ArgumentParser(description="CZN GameKee Wiki 数据抓取器")
    parser.add_argument("--output", "-o", type=str, default=".",
                        help="输出目录，JSON 文件将写入此目录")
    parser.add_argument("--huodong-html", type=str,
                        help="预先抓取的 huodong 页面 HTML/文本文件路径")
    parser.add_argument("--kachi-html", type=str,
                        help="预先抓取的 kachi 页面 HTML/文本文件路径")
    parser.add_argument("--from-stdin", action="store_true",
                        help="从 stdin 读取 JSON 数据 (用于 AI Agent 协作)")
    parser.add_argument("--dry-run", action="store_true",
                        help="仅解析不写入，输出到 stdout")
    parser.add_argument("--guide", action="store_true",
                        help="显示手动更新指南")
    args = parser.parse_args()
    
    if args.guide:
        print("""
============================================================
卡池数据手动更新指南
============================================================

kachi 页面是 JS 渲染的 SPA，需使用 Playwright 后端渲染后抓取:

1. 使用 AI Agent 的 web_fetch (playwright 后端) 获取页面内容
2. 将内容保存为文本文件
3. 运行: python gamekee_scraper.py --kachi-html kachi.txt --output ./data/

或直接手动更新 banners.json:
{
  "id": N,
  "name": "角色名",
  "stars": 5,
  "element": "属性(风/寒/热/光/暗)",
  "className": "职业",
  "type": "current|history|upcoming",
  "startDate": "YYYY-MM-DD",
  "endDate": "YYYY-MM-DD",
  "url": "https://www.gamekee.com/czn/kachi/104",
  "server": "国际服"
}
============================================================
""")
        return
    
    activities = []
    banners = []
    
    # 模式1: 从 stdin 读取 (AI Agent 协作)
    if args.from_stdin:
        raw = sys.stdin.read()
        data = json.loads(raw)
        activities = data.get("activities", [])
        banners = data.get("banners", [])
    
    # 模式2: 从 HTML 文件解析
    else:
        if args.huodong_html:
            with open(args.huodong_html, "r", encoding="utf-8") as f:
                html = f.read()
            activities = parse_activities_from_text(html)
        
        if args.kachi_html:
            with open(args.kachi_html, "r", encoding="utf-8") as f:
                html = f.read()
            banners = parse_banners_from_text(html)
    
    # 如果没有指定 HTML 文件，尝试 HTTP 直接抓取 (通常失败)
    if not activities and not banners and not args.from_stdin:
        print("[INFO] 未指定 HTML 文件，尝试 HTTP 直接抓取...", file=sys.stderr)
        activity_html = http_get(ACTIVITY_URL)
        if activity_html:
            activities = parse_activities_from_text(activity_html)
        banner_html = http_get(BANNER_URL)
        if banner_html:
            banners = parse_banners_from_text(banner_html)
    
    # 格式化和验证
    activities = validate_and_format_activities(activities)
    banners = validate_and_format_banners(banners)
    
    if not activities and not banners:
        print("[WARN] 未抓取到任何数据。请使用 --huodong-html / --kachi-html 指定预抓取文件", file=sys.stderr)
        return
    
    # 输出
    if args.dry_run:
        result = {"activities": activities, "banners": banners}
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        if activities:
            write_json(activities, os.path.join(args.output, "events.json"))
        if banners:
            write_json(banners, os.path.join(args.output, "banners.json"))
        
        # 打印摘要
        print(f"\n[摘要] 活动: {len(activities)} 条, 卡池: {len(banners)} 条", file=sys.stderr)
        for a in activities[:5]:
            print(f"  [活动] {a['name']} → {a['endDate']}", file=sys.stderr)
        for b in banners[:5]:
            print(f"  [卡池] {b['name']} ({b['type']}) → {b['endDate']}", file=sys.stderr)


if __name__ == "__main__":
    main()