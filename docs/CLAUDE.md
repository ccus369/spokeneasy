# SpokenEasy - CLAUDE 工作指引

## 项目概述
Android 英语口语练习 App "SpokenEasy"，Java + XML Layouts + Navigation Component + Room。

## 项目文档路径
- [需求文档](01-requirements.md)
- [技术规范](02-technical-spec.md)
- [架构设计](03-architecture.md)
- [数据库设计](04-data-schema.md)

## 工作说明

### 开发原则
1. **动手前先想**：不确定就问，不假装懂了瞎干
2. **简洁优先**：50行能解决别写200行，不引入不必要的抽象
3. **精准修改**：只动必要的代码，不顺手改格式/清旧代码
4. **完工必验证**：改完必须自测，编译+功能双确认
5. **增量验证**：改一点→编译确认→再继续，避免累积问题
6. **先读再写**：改不熟悉的模块前先读通现有逻辑，不盲改
7. **明确假设**：依赖外部条件时写清楚假设
8. **暴露冲突**：逻辑矛盾时主动暴露讨论，不悄悄选方案
9. **拒绝复杂**：遇到嵌套深/耦合重的设计，先重构再继续
10. **不做预期功能**：现在不需要就不写
11. **不藏错误**：出错就报，不吞异常静默失败
12. **明确失败**：失败信息要具体，不让调用方猜
13. **按功能分包**：word/linking/listening/...各自独立
14. **同步更新本文档**：新增功能或改架构时更新

### 代码规范
- Java 17
- MVVM 模式：Fragment → ViewModel → Repository → DAO
- Fragment 文件后缀 `*Fragment.java`
- ViewModel 文件后缀 `*ViewModel.java`
- 布局文件 `fragment_*.xml` / `activity_*.xml` / `item_*.xml`
- 菜单文件 `menu/*.xml`
- 导航文件 `navigation/nav_graph.xml`
- 网络服务 `core/net/*Service.java`（如 MiMoApiService）
- 网络配置 `core/net/*Config.java`（如 ApiConfig）

### 关键架构决策
- 预置 .db 文件：`assets/database/spokeneasy.db`（SQLite 可视化工具创建）
- 导航：DrawerLayout + BottomNavigationView + NavHostFragment
- Navigation Component 导航图 + NavigationUI 自动同步
- 评分：Scorer 接口 → MockScorer(模拟) / XunfeiScorer(讯飞 ISE SDK)
- 音频：TTSEngine + AudioRecorder 独立封装
- 主题：浅色+深色模式，Material3 XML 主题，蓝色主色调
- 用户：设备UUID标识，仅游客模式
- AI 聊天：MiMo API（`api.xiaomimimo.com/v1/chat/completions`，OpenAI 兼容），OkHttp 4.12.0
- 练习记录：PracticeRecordEntity Room 表（user_uuid + created_at 索引），DB migration v1→2
- TTS 引擎回退链（国产 ROM 兼容）：默认引擎 → `getEngines()` → `Settings.Secure.tts_default_synth` → 已知 OEM 引擎（OPPO/Xiaomi/Huawei/Vivo）。`boolean[] listenerFired + int[] initStatus` 数组模式处理同步回调竞争
- 首页容器：LearnFragment + PracticeRootFragment 使用 TabLayout + ChildFragmentManager 实现标签切换，避免 Navigation Component 嵌套
- 发音实验室：Json 驱动的极小对立体（minimal pairs）数据模型，按音素分类筛选，ISE 评分对比练习
- 句型操练：四类题型共用 DrillStep 数据模型，base/cue/expected 三段式驱动；`DrillCollection → DrillSet → DrillStep` 三层嵌套 JSON 结构
- 情景对话：五阶段状态机（SCENE_SELECT→WARMUP→DIALOGUE→ROLEPLAY→SUMMARY），角色扮演复用 MiMoApiService 进行 AI 对话

### Phase 进度
- Phase 1 ✅ 项目骨架搭建（Gradle 配置 + 导航框架 DrawerLayout/BottomNavigation/NavHost + 占位页 + Material3 主题）
- Phase 2 ✅ 基础设施（5 Entity + 4 DAO + AppDatabase + assets/database/spokeneasy.db + TTS/AudioRecorder/Scorer/UuidManager 工具类）
- Phase 3 ✅ 单词板块（WordRepository/ViewModel/ListAdapter/ListFragment/DetailFragment，完整 TTS→录音→评分闭环）
- Phase 4 ✅ 连读板块（LinkingEntity/DAO/Repository/ViewModel/ListAdapter/ListFragment/DetailFragment，规则+例句+跟读练习）
- Phase 5 ✅ 听力板块（AudioEntity+QuestionEntity 双表设计 + ChipGroup 难度筛选 + RadioGroup 3 题自动批改）
- Phase 6 ✅ 设置 + 统计模块（UserProgressRepository/ViewModel，设置页学习统计卡片、ProgressBar、数据导出/重置、设备UUID显示）
- Phase 5.5 ✅ UI 全面改造 + 听力模块重构（TTS 播放对话、隐藏原文、微动画系统、3 模块列表/详情 UI 升级）
- Phase 7 ✅ 讯飞 ISE 语音评测 SDK（XunfeiScorer 真实评分替换 MockScorer + AudioWaveformView 波形可视化 + TTS 引擎缓存/队列/语言检测）
- Phase 8 ✅ 练习记录 + 系统设置 + AI 聊天（PracticeRecord 历史回放 + TTS 四态检测面板 + 多引擎回退链/OEM 兼容 + MiMo API AI 英语语伴）
- Phase 9a ✅ 首页重构（LearnFragment TabLayout 单词/连读 + PracticeRootFragment TabLayout 发音/句型/对话）
- Phase 9b ✅ 发音实验室（最小对立体 minimal_pairs.json + 音素分类 ChipGroup + 对比发音 + ISE 评分）
- Phase 9c ✅ 句型操练（4 种题型 substitution/transformation/expansion/response + 6 语法点 JSON 题库 + 3 阶段: 选择→操练→总结）
- Phase 9d ✅ 情景对话（5 阶段: 场景选择→预热词汇→对话跟读→AI 角色扮演→总结报告 + MiMo API 驱动角色扮演）

---

## 版本勘误（重要）

`02-technical-spec.md` 中的版本号是**初始计划**，实际已验证可用的版本为：

| 配置项 | 文档写的（别用） | 已验证可用的 |
|--------|----------------|------------|
| Gradle | 9.4.1 | 8.14.5 |
| AGP | 9.2.1 | 8.2.2 |
| Kotlin | 2.0.x | 1.9.22（已移除，转 Java 17） |
| Compose BOM | 2024.x | 2024.02.00（已移除，转 XML） |

**不要用 Gradle 9.x / AGP 9.x**，Gradle 9 移除了部分 API，会导致编译错误。

---

## Build 踩坑记录

| 问题 | 原因 | 正确做法 |
|------|------|---------|
| `NoSuchMethodError: module(Object)` | Gradle 9.x 移除了 API | 用 Gradle 8.14.5 + AGP 8.2.2 |
| 插件找不到 `com.android.application` | settings.gradle.kts 的 google() 加了 content 过滤 | 去掉 content 过滤 |
| Gradle 下载超时 | 国内网络问题 | 用腾讯云镜像下载，手动放入缓存 |
| SHA256 校验失败 | 用了错误版本的 checksum | 版本确定后从 Gradle 官网获取正确 checksum |
| Theme.Material3.DayNight.NoActionBar 找不到 | 缺少 `com.google.android.material:material` 依赖 | 添加 Material Components 依赖 |
