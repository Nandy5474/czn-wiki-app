---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: 0078dcf006a297d188fa25ba29952721_75b49ebc649c11f196be5254006c9bbf
    ReservedCode1: zQIaoz0RvkmpZD9H9LcvhkCEq3NDzHaQ9BUODyF9zLL8qPncXLW2HFbxI5H0NvEjuNsKU1+YkUUAbccy/voKcykZk0lKWE+zSPJ0pFYJfTTkPk4+Ri6ce3VekM4EWGJdtXzMc5f0T0StxSJv0pYtCZKGkFdn80bKRVaRq1qkyh2UvtFlqo9Ov1e36R8=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: 0078dcf006a297d188fa25ba29952721_75b49ebc649c11f196be5254006c9bbf
    ReservedCode2: zQIaoz0RvkmpZD9H9LcvhkCEq3NDzHaQ9BUODyF9zLL8qPncXLW2HFbxI5H0NvEjuNsKU1+YkUUAbccy/voKcykZk0lKWE+zSPJ0pFYJfTTkPk4+Ri6ce3VekM4EWGJdtXzMc5f0T0StxSJv0pYtCZKGkFdn80bKRVaRq1qkyh2UvtFlqo9Ov1e36R8=
---

# 卡厄思梦境 Wiki App 开发日志

## 项目概述
基于 GameKee 数据的《卡厄思梦境》Wiki Android 应用，提供角色图鉴、卡牌查询、自意识（命座）管理、收藏系统等功能。仅包含国际服（EROLABS 平台）数据。

## 开发历程

### 2026-06-10 14:24
**版本 v2.0.25** - 启动竞态修复 + 字段映射 + 数据补全
- 修复 CznApplication 启动竞态条件（seedDatabaseFromAssets 异步导致数据未写入）
- 修复 characters.json 中 "class" 字段到 CharacterEntity "job" 字段的映射
- 补全 LocalDataManager 中 events 和 banners 数据的导入逻辑
- 对齐版本号：versionCode=25, versionName="2.0.25"
- 创建 gamekee_scraper.py 脚本从 GameKee 抓取 36 条活动数据和 6 条卡池数据

### 2026-06-10 12:52
**版本 v2.0.23** - events.json 全量刷新 + 解析修复
- events.json：GameKee 全量刷新 36 条活动数据
- banners.json：纯 CZN 国际服数据
- 修复 gamekee_scraper.py 解析 bug（日期格式错误、名称截断）

### 2026-06-10 12:04
**版本 v2.0.23** - 数据字段映射 + 国服清理
- 修复角色数据字段映射
- 移除所有国服相关数据
- 补全国际服卡池（14 个）和活动（5 个）

### 2026-06-10 11:40
**版本 v2.0.22** - 国际服数据 + 抽卡记录 + 数据备份
- 添加国际服专属数据
- 新增数据备份还原功能
- 新增抽卡记录功能
- 修复 BuildConfig 问题

### 2026-06-10 09:48
**版本 v2.0.22** - cards.json 全面修复
- sortOrder 1-8 标准排列（原 1/2/3 混叠）
- cardType 正确标注：基础卡(99) + 灵光一闪(132) + 独特(33)
- 补全蕾伊(id=24)、欧文(id=30) 缺失 16 张卡
- cardId 统一格式 `{characterId}_{sortOrder}`
- 必填字段 cost/effect/name 全部补全
- 总计 264 张卡（33×8）

### 2026-06-10 00:16
**版本 v2.0.21** - 首页跳转 + 详情页角色切换
- HomeScreen：角色图鉴快速入口改为跳转 CharacterList 而非固定角色
- CharacterDetailScreen：TopAppBar 新增左/右箭头切换上一个/下一个角色
- 顶部居中显示当前序号/总数
- CharacterDetailViewModel 新增 allCharacterIds 支持切换
- CharacterDao 新增 getAllCharacterIdsSync()

### 2026-06-10 00:02
**版本 v2.0.20** - 全角色基础信息 + SA 数据修复
- characters.json：33 角色补齐阵营/种族/CV/生日/基础三围/稀有度/描述
- self_awareness.json：修复角色 SA 串位，22 角色完整 6 条，11 角色缺 stage3（Wiki 源）
- ID 映射修正：Wiki 顺序重映射为项目固定 ID
- 数据源：B站 Wiki（wiki.biligame.com/czn）

### 2026-06-09 14:26
**数据修复** - 安洁莉卡 SA 数据修正
- 蒂菲拉（安洁莉卡）自我意识数据从黛安娜交叉数据修正为创造卡牌体系

### 2026-06-09 13:13
**功能新增** - 数据分层架构
- 新增 LocalDataManager.kt：三层数据架构管理器（版本管理 + 用户修改层共享存储 + 数据更新流程）
- AppDatabase 各 Dao 添加 deleteAll()
- CznApplication 集成 LocalDataManager，启动时检查版本并自动更新数据
- RemoteUpdateManager：远程更新前后保存/回灌用户修改，不再用远程 user_collection 覆盖本地数据
- CharacterDetailViewModel / CollectionViewModel：用户编辑同时持久化到 LocalDataManager
- 新增 assets/data/version.json：APK 内嵌数据版本号

### 2026-06-09 12:25
**数据修复** - 海德玛丽 SA 数据修正
- 海德玛丽 SA 数据修正 + 赛雷妮尔/安洁莉卡结构修复

### 2026-06-09 10:46
**文档** - 添加 README
- 添加 AI-assisted project disclaimer

### 2026-06-09 09:34
**数据补充** - SA 第二批（6 个角色）
- 娜嘉(id=8)：贪婪/掠食机制
- 千鹤(id=10)：月影/鬼火/凝聚机制
- 友纪(id=11)：灵感/剑舞机制
- 琳(id=13)：黑云态势/剑法机制
- 麦格纳(id=16)：反击/冰冻/严寒机制
- 奥尔莱亚(id=17)：创造物/保留机制
- 数据来源：ali213 游侠手游攻略页

### 2026-06-09 01:39
**数据补充** - SA 第一批（5 个角色）
- 阿黛海特、赛雷妮尔、路克、凯西乌斯、蕾伊自我意识数据
- 海德玛丽角色属性补全

### 2026-06-08 17:56
**版本 v2.0.16** - 构建修复
- versionCode 18
- 修复 6-flow combine 类型推断问题

### 2026-06-08 17:31
**功能优化** - 应用更名 + 梯度同步
- 应用名改为「卡厄思wiki」
- 强度梯度同步修复
- 新增 SA 补充分批计划

### 2026-06-08 17:23
**版本 v2.0.15** - 33 角色扩展
- 31→33 角色 ID 映射修复
- 新增绯、凯隆卡牌数据
- 自我意识阶段命名统一

### 2026-06-08 15:07
**版本 v2.0.14** - 版本号解析 + 更新提示
- 修复版本号比较解析逻辑
- 新增更新进度提示
- 底部显示上次更新时间

### 2026-06-08 14:37
**版本 v2.0.13** - 版本号提升

### 2026-06-08 11:54
**版本 v2.0.12** - 立绘修复
- 修复立绘错位问题
- 角色图鉴图片比例自适应
- 同步角色图鉴立绘编号与 characters.json 对应

### 2026-06-08 10:23
**版本 v2.0.9** - 数据源迁移
- 修复更新服务器连接失败问题
- 数据源迁移至 czn-wiki-app/data/
- 更新 versionCode=11

### 2026-06-06 09:32
**版本 v2.0.8** - 立绘映射修复
- 按角色名重新匹配 README 编号到 characters.json ID

### 2026-06-06 05:44
**资源合并** - czn-wiki-data 仓库合并
- 合并 31 张立绘（png）+ 31 张缩略图（webp）+ JSON 数据文件 + README + version

### 2026-06-06 05:32
**版本 v2.0.7** - 立绘集成
- 修复 T0 角色 ID 映射
- 集成本地 webp 立绘（300×462）
- 新增 Coil AssetUriFetcher

### 2026-06-05 14:30
**版本 v8** - 角色数据完善与首页跳转修复
- 修复首页 T0 角色跳转 ID 错位问题（奥尔莱雅 1→17、维诺妮卡 2→13 等）
- 补充 8 个占位角色数据：奈音、卢卡斯、德蕾莎、雷伊、阿黛海特、艾美、玛莉贝儿、欧文
- 为 6 个角色补充立绘图片（char_04/06/14/17/20/24）
- 构建 APK MD5: a116ac298bbd5240e100d3dc78f1bbe9

### 2026-06-05 13:57
**版本 v7** - 数据核对与 UI 优化
- 修复 self_awareness.json 前 21 条数据 characterId 错位问题
- 清除 165 条伪造模板数据，替换为 31×6 结构
- 通过 browser agent 抓取角色详情，补充 8 个占位角色的 element/job 属性
- 精简 CardListTab 过滤逻辑

### 2026-06-05 12:19
**版本 v6** - gamekee 卡牌数据全量同步
- 通过 browser agent 抓取全部 31 角色×10 卡牌=310 张卡牌数据
- 修复 5 个错误的角色 URL 映射（琳、小春、奥尔莱雅、百丽儿、赛雷尼尔）
- 重新生成 cards.json（310 张：155 基础 + 124 灵光一闪 + 31 独特）
- 自动解析效果文本标签：[唯一]→isUnique、[保留]→isRetain 等

### 2026-06-05 10:31
**版本 v5** - 闪退修复与数据完整性
- 修复 Room @Database version 1→2，触发 fallbackToDestructiveMigration
- 修复 updateOwnership 方法覆盖已有 Tier/命座数据问题
- 修复 thumbUrl/imageUrl 为空时 Coil 解析异常

### 2026-06-05 09:31
**版本 v4** - 远程同步实现
- 扩展 RemoteUpdateManager 支持拉取 user_collection.json
- 更新 HomeScreen 对话框显示收藏更新数
- 生成 version.json v5，更新 build.gradle versionCode 5→6

### 2026-06-05 09:12
**版本 v3** - 收藏系统 + APK 构建 + 远程同步
- 新增收藏系统：UserCollectionEntity 添加 customTier 字段
- 生成 31 条种子数据（user_collection.json）
- CharacterDetailScreen 新增「收藏管理」卡片
- CollectionScreen 重写为 LazyColumn + 展开编辑面板
- 构建 APK v2.0.4（16MB）
- 研究远程同步方案：GitHub 种子数据同步 + 用户编辑跨设备同步

### 2026-06-04 17:03
**版本 v2** - 自意识（命座）系统
- 实现 self_awareness.json 数据加载与展示
- CharacterDetailScreen 新增 SelfAwarenessTab

### 2026-06-04 15:49
**版本 v1** - 基础功能
- 角色图鉴列表/网格视图
- 角色详情页（基础信息、卡牌列表）
- 卡牌搜索与过滤
- 基础数据加载（characters.json、cards.json）

## 技术架构
- 语言：Kotlin
- UI：Jetpack Compose
- 数据库：Room
- 网络：Retrofit + OkHttp
- 图片：Coil
- 架构：MVVM + Repository 模式
- 数据分层：LocalDataManager 三层架构（基础层 + 用户修改层 + 远程更新层）
- 构建：JDK 17（/private/tmp/jdk-17.0.19+10/Contents/Home）

## 数据源
- characters.json：33 个角色基础信息（国际服）
- cards.json：264 张卡牌（99 基础 + 132 灵光一闪 + 33 独特，33×8 结构）
- self_awareness.json：33×6=198 条自意识数据（部分 stage3 待补全）
- user_collection.json：用户收藏种子数据
- events.json：38 条活动数据（GameKee 源）
- banners.json：7 条卡池数据（GameKee 源，纯国际服）

## 远程同步
- 数据版本管理：version.json
- 支持增量更新：角色、卡牌、自意识、用户收藏、活动、卡池
- GitHub 仓库：Nandy5474/czn-wiki-data
- 用户修改持久化：LocalDataManager 保存/回灌机制

## 待办事项
- [ ] 补充玛莉贝儿、欧文角色立绘
- [ ] 补全 11 个角色缺失的 stage3 自意识数据
- [ ] 实现阵容推荐功能
- [ ] 添加当期活动展示模块
- [ ] 优化 UI/UX 体验
- [ ] 抽卡记录云端同步

## v2.0.26 (2026-06-10)
- **卡池数据修正**：删除错误黛安娜复刻，补充凯隆、维罗妮卡等7条卡池数据
- **活动数据刷新**：新增2个活动，共38条国际服活动数据
- **职业筛选修复**：ViewModels.kt 新增"前锋"和"操控师"职业筛选，与UI兼容
- **版本对齐**：所有 version.json 和 build.gradle.kts 版本号统一为26
- **数据验证**：characters.json 33个角色数据完整，element/class字段正确
*（内容由AI生成，仅供参考）*
