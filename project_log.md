# 卡厄思梦境 Wiki App 开发日志

## 项目概述
基于 GameKee 数据的《卡厄思梦境》Wiki Android 应用，提供角色图鉴、卡牌查询、自意识（命座）管理、收藏系统等功能。

## 开发历程

### 2026-06-05 14:30
**版本 v8** - 角色数据完善与首页跳转修复
- 修复首页 T0 角色跳转 ID 错位问题（奥尔莱雅 1→17、维诺妮卡 2→13 等）
- 补充8个占位角色数据：奈音、卢卡斯、德蕾莎、雷伊、阿黛海特、艾美、玛莉贝儿、欧文
- 为6个角色补充立绘图片（char_04/06/14/17/20/24）
- 构建 APK MD5: a116ac298bbd5240e100d3dc78f1bbe9

### 2026-06-05 13:57
**版本 v7** - 数据核对与UI优化
- 修复 self_awareness.json 前21条数据 characterId 错位问题
- 清除165条伪造模板数据，替换为31×6结构
- 通过 browser agent 抓取角色详情，补充8个占位角色的element/job属性
- 精简 CardListTab 过滤逻辑
- 构建 APK MD5: 04ea5aedf93c997b3b603a4579aa3c9e

### 2026-06-05 12:19
**版本 v6** - gamekee卡牌数据全量同步
- 通过 browser agent 抓取全部31角色×10卡牌=310张卡牌数据
- 修复5个错误的角色URL映射（琳、小春、奥尔莱雅、百丽儿、赛雷尼尔）
- 重新生成 cards.json（310张：155基础+124灵光一闪+31独特）
- 自动解析效果文本标签：[唯一]→isUnique、[保留]→isRetain等
- 构建 APK MD5: 98e10733cf9566b892ba45da7810cc67（v5重命名）

### 2026-06-05 10:31
**版本 v5** - 闪退修复与数据完整性
- 修复 Room @Database version 1→2，触发 fallbackToDestructiveMigration
- 修复 updateOwnership 方法覆盖已有 Tier/命座数据问题
- 修复 thumbUrl/imageUrl 为空时 Coil 解析异常
- 构建 APK MD5: 98e10733cf9566b892ba45da7810cc67

### 2026-06-05 09:12
**版本 v4** - 远程同步实现
- 扩展 RemoteUpdateManager 支持拉取 user_collection.json
- 更新 HomeScreen 对话框显示收藏更新数
- 生成 version.json v5，更新 build.gradle versionCode 5→6
- 用户需将 user_collection.json/version.json 推送至 GitHub 仓库

### 2026-06-05 09:31
**版本 v3** - 收藏系统 + APK构建 + 远程同步研究
- 新增收藏系统：UserCollectionEntity 添加 customTier 字段
- 生成31条种子数据（user_collection.json）
- CharacterDetailScreen 新增「收藏管理」卡片
- CollectionScreen 重写为 LazyColumn + 展开编辑面板
- 构建 APK v2.0.4（16MB）
- 研究远程同步方案：GitHub 种子数据同步 + 用户编辑跨设备同步

### 2026-06-04 17:03
**版本 v2** - 自意识（命座）系统
- 实现 self_awareness.json 数据加载与展示
- CharacterDetailScreen 新增 SelfAwarenessTab
- 构建 APK MD5: 16859827

### 2026-06-04 15:49
**版本 v1** - 基础功能
- 角色图鉴列表/网格视图
- 角色详情页（基础信息、卡牌列表）
- 卡牌搜索与过滤
- 基础数据加载（characters.json, cards.json）

## 技术架构
- 语言：Kotlin
- UI：Jetpack Compose
- 数据库：Room
- 网络：Retrofit + OkHttp
- 图片：Coil
- 架构：MVVM + Repository 模式

## 数据源
- characters.json：31个角色基础信息
- cards.json：310张卡牌（155基础+124灵光一闪+31独特）
- self_awareness.json：31×6=186条自意识数据
- user_collection.json：31条用户收藏种子数据

## 远程同步
- 数据版本管理：version.json
- 支持增量更新：角色、卡牌、自意识、用户收藏
- GitHub 仓库：Nandy5474/czn-wiki-data

## 待办事项
- [ ] 补充玛莉贝儿、欧文角色立绘
- [ ] 完善所有角色的 faction/rarity/cv/race/birthday 数据
- [ ] 添加 baseAtk/baseDef/baseHp 数值
- [ ] 实现阵容推荐功能
- [ ] 添加当期活动模块
- [ ] 优化 UI/UX 体验