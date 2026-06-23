# CZN Wiki App 开发日志

## v2.0.35 - 2026-06-22

### 变更内容

**自我意识数据补全**

- self_awareness.json 从 193 条增至 204 条（新增 id 194-204）
- 补全 11 个角色的 stage 3「鲜明的记忆」：凯西乌斯、妮雅、卢卡斯、百丽儿、德蕾莎、蕾伊、米卡、艾美、席琳娜、玛丽贝尔、欧文
- 效果统一为：潜力的【强化基本卡牌】、【强化独特卡牌】、【强化中立卡牌】的等级增加 3
- 34 个角色 self_awareness 现均为 stage 1-6 完整

### 文件变更
- data/self_awareness.json
- data/version.json
- app/src/main/assets/data/self_awareness.json
- app/src/main/assets/data/version.json
- APK: czn-wiki-v2.0.35-release.apk (v2+v3 签名)

## v2.0.28 - 2026-06-10

### 变更内容

**数据完整性修复**

1. **banners.json 全量重建为12条**：补充用户模板中填写的全部12条卡池记录
   - 新增：蒂菲拉、奈音、娜嘉、赛雷妮尔、千鹤、友纪、小春（7条历史卡池）
   - 保留：丽塔、黛安娜、海德玛丽、阿黛海特、泰妮布里亚（5条已有记录）
   - 泰妮布里亚 character 从 `?` 补全为 `热情+心灵术士`

2. **characters.json 职业名称统一为 Gamekee 国际服命名**（共21个角色）：
   - 奥义师 → 心灵术士（6角色：丽塔、千鹤、德蕾莎、凯隆、泰妮布里亚；雨果已是游侠跳过）
   - 操控师 → 控制师（7角色：蒂菲拉、娜嘉、奥尔莱亚、凯西乌斯、妮雅、蕾伊、米卡）
   - 前锋 → 格斗家（5角色：友纪、小春、琳、梅铃、欧文）
   - 守卫 → 先锋（4角色：奈音、麦格纳、卡莉佩、艾美）
   - data/characters.json 副本同步更新

3. **ViewModels.kt 职业过滤器修正**：
   - jobs 列表移除旧名（前锋、操控师、奥义师、守卫）
   - 新增格斗家
   - 最终列表：决斗家、先锋、格斗家、游侠、猎人、控制师、心灵术士

### 文件变更
- app/src/main/assets/data/banners.json
- app/src/main/assets/data/characters.json
- app/src/main/java/com/cznwiki/app/ui/viewmodel/ViewModels.kt
- data/characters.json

### 已知待确认
- 角色图鉴空数据问题：Entity 与 JSON 字段映射已验证正确，怀疑 seedDatabaseFromAssets 调用链或数据库迁移时机问题
- GitHub push 超时，commit 已在本地（8c07e8a），待网络恢复后推送

## v2.0.32 (2026-06-11)

- versionCode=32, versionName="2.0.32", APK 3.0MB
- 修复角色立绘丢失：characters.json 中角色 ID 1~33 的 imageUrl/thumbUrl 从远程 CDN 改为本地 asset 路径（file:///android_asset/data/char_{id}.webp），角色 34（泰妮布里亚）无本地文件保持空字符串
- 基于 v2.0.31 commit 14bd067 构建，commit 67700ba，已推送失败（GitHub 网络不通），本地仓库已就绪

## v2.0.33 (2026-06-11)

- versionCode=33, versionName="2.0.33", Release APK 3.0MB（已签名 + R8 混淆）
- 队伍构筑 UI 增强：创完队伍后每个角色显示圆形立绘（72dp Coil AsyncImage 加载 thumbUrl），无立绘时回退为首字母渐变圆形
- 点击队伍角色立绘跳转角色详情页（onNavigateToDetail 接入）
- 编译警告清零：CharacterListScreen.kt 弃用图标添加 @Suppress("DEPRECATION")，TeamScreen.kt onNavigateToDetail 参数接入

## v2.0.34 (2026-06-11)

- versionCode=34, versionName="2.0.34", Release APK
- 筛选面板修复：Row → FlowRow 解决选项溢出截断问题
- 星级筛选修正：仅保留 4 星和 5 星（移除其他无效星级）
- 职业筛选修正：对齐游戏实际 6 职业（游侠、猎人、先锋、决斗家、心灵术士、控制师），移除"格斗家"
- 签名密钥迁移：/tmp → 项目根目录（czn-wiki-release.keystore），避免系统清理导致签名失败
