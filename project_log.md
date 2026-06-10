# CZN Wiki App 开发日志

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
