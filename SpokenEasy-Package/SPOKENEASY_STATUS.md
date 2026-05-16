# SpokenEasy — 项目状态与开发计划

## ⚠️ 版本勘误（重要）

`docs/02-technical-spec.md` 中的版本号是**初始计划**，实际已验证可用的版本为：

| 配置项 | 文档写的（别用） | 已验证可用的 |
|--------|----------------|------------|
| Gradle | 9.4.1 | 8.14.5 |
| AGP | 9.2.1 | 8.2.2 |
| Kotlin | 2.0.x | 1.9.22 |
| Compose BOM | 2024.x | 2024.02.00 |

**不要用 Gradle 9.x / AGP 9.x**，Gradle 9 移除了部分 API，会导致编译错误。

---

## 给新对话的启动指令

1. 先完整阅读本文件和 CLAUDE.md
2. 再读 docs/ 下 4 份文档
3. 从 Phase 1 开始逐阶段实现
4. 每完成一个模块 → 编译验证 → 再继续

---

## 完整开发计划（6 个阶段）

### Phase 1：项目骨架搭建
- Gradle 配置（AGP / Kotlin / Room / Compose 版本对齐）
- Application + MainActivity
- 主题系统（浅色+深色）
- 导航框架（Drawer + BottomNav + NavHost）
- AndroidManifest
- **验证标准：App 能跑起来，导航能切换**

### Phase 2：基础设施
- 所有 Entity + DAO（word/linking/listening/progress）
- AppDatabase（Room + createFromAsset）
- assets/database/spokeneasy.db（预置数据库文件）
- core/model/Common.kt（公共数据类）
- TTSEngine + AudioRecorder
- Scorer 接口 + MockScorer
- UuidManager
- **验证标准：数据库能初始化，工具类可调用**

### Phase 3：单词板块
```
word/
├── WordRepository.kt       # 数据仓库
├── WordViewModel.kt        # ViewModel
├── WordListScreen.kt       # 单词列表页
└── WordDetailScreen.kt     # 单词详情+例句+跟读
```
- 完成标准：跟读评分 ≥ 60 分

### Phase 4：连读板块
```
linking/
├── LinkingRepository.kt
├── LinkingViewModel.kt
├── LinkingListScreen.kt
└── LinkingDetailScreen.kt
```
- 完成标准：练习评分 ≥ 60 分

### Phase 5：听力板块
```
listening/
├── ListeningRepository.kt
├── ListeningViewModel.kt
├── ListeningListScreen.kt
└── ListeningDetailScreen.kt
```
- 完成标准：答题 ≥ 60 分

### Phase 6：设置 + 统计
```
settings/
└── SettingsScreen.kt
progress/
├── UserProgressRepository.kt
└── ProgressViewModel.kt
```

---

## Build 历程总结（已踩过的坑，避免再踩）

| 问题 | 原因 | 正确做法 |
|------|------|---------|
| `kotlin("plugin.compose")` 找不到 | 漏了 Compose 插件依赖 | 加 `kotlin("plugin.compose")` 并指定版本 |
| `NoSuchMethodError: module(Object)` | Gradle 9.x 移除了 API | 用 Gradle 8.14.5 + AGP 8.2.2 |
| 插件找不到 `com.android.application` | settings.gradle.kts 的 google() 加了 content 过滤 | 去掉 content 过滤 |
| Gradle 下载超时 | 国内网络问题 | 用腾讯云镜像下载，手动放入缓存 |
| SHA256 校验失败 | 用了错误版本的 checksum | 版本确定后从 Gradle 官网获取正确 checksum |