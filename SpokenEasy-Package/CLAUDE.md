# SpokenEasy - CLAUDE 工作指引

## 项目概述
Android 英语口语练习 App "SpokenEasy"，Java + XML Layouts + Navigation Component + Room。

## 项目文档路径
- [需求文档](docs/01-requirements.md)
- [技术规范](docs/02-technical-spec.md)
- [架构设计](docs/03-architecture.md)
- [数据库设计](docs/04-data-schema.md)
- [项目状态与计划](SPOKENEASY_STATUS.md)

## 工作说明

### 开发原则
1. **逐阶段推进**：完成→编译验证→进入下一阶段，不跳步
2. **每次只改一个模块**：不跨模块修改
3. **编译通过是硬门槛**：任何修改后必须能编译
4. **按功能分包**：word/linking/listening/settings/progress 各自独立
5. **同步更新状态文档**：每完成一个阶段/模块，同步更新本方文件 (§Phase 进度) 和 `SPOKENEASY_STATUS.md`

### 代码规范
- Java 17
- MVVM 模式：Fragment → ViewModel → Repository → DAO
- Fragment 文件后缀 `*Fragment.java`
- ViewModel 文件后缀 `*ViewModel.java`
- 布局文件 `fragment_*.xml` / `activity_*.xml`
- 菜单文件 `menu/*.xml`
- 导航文件 `navigation/nav_graph.xml`

### 关键架构决策
- 预置 .db 文件：`assets/database/spokeneasy.db`（SQLite 可视化工具创建）
- 导航：DrawerLayout + BottomNavigationView + NavHostFragment
- Navigation Component 导航图 + NavigationUI 自动同步
- 评分：Scorer 接口 → MockScorer(模拟) / XunfeiScorer(讯飞)
- 音频：TTSEngine + AudioRecorder 独立封装
- 主题：浅色+深色模式，Material3 XML 主题，蓝色主色调
- 用户：设备UUID标识，仅游客模式

### Phase 进度
- Phase 1 ✅ 项目骨架搭建（Gradle 配置 + 导航框架 DrawerLayout/BottomNavigation/NavHost + 占位页 + Material3 主题）
- Phase 2 ✅ 基础设施（5 Entity + 4 DAO + AppDatabase + assets/database/spokeneasy.db + TTS/AudioRecorder/Scorer/UuidManager 工具类）
- Phase 3 ✅ 单词板块（WordRepository/ViewModel/ListAdapter/ListFragment/DetailFragment，完整 TTS→录音→评分闭环）
- Phase 4 ✅ 连读板块（LinkingEntity/DAO/Repository/ViewModel/ListAdapter/ListFragment/DetailFragment，规则+例句+跟读练习）
- Phase 5 ✅ 听力板块（AudioEntity+QuestionEntity 双表设计 + ChipGroup 难度筛选 + RadioGroup 3 题自动批改）
- Phase 6 ⏳ 部分完成（SettingsFragment 占位 + UserProgressEntity/DAO 基础，缺少进度展示 UI 和 Repository/ViewModel）
