# SpokenEasy - 架构设计

> 📅 最后更新：2026-06-01

## 总体架构模式：MVVM + Repository

```
[Fragment/XML UI] → [ViewModel] → [Repository] → [Room Database / TTS / Scorer]
     ↑                  |                |
     └── LiveData ──────┘                └── 数据唯一来源
```

## 分包结构

```
com.spokeneasy.app/
├── SpokenEasyApp.java              # Application（组件初始化、未捕获异常处理）
├── MainActivity.java               # 入口 Activity（DrawerLayout + BottomNav + NavHost）
│
├── home/                           # 首页容器（Phase 9a）
│   ├── GuideFragment.java          # 引导页（逐项弹入动画）
│   ├── LearnFragment.java          # 学习页（TabLayout: 单词/连读/Shadowing）
│   └── PracticeRootFragment.java   # 练习页（TabLayout: 发音/句型/对话）
│
├── word/                           # 单词跟读模块（Phase 3）
│   ├── WordEntity.java
│   ├── WordDao.java
│   ├── WordRepository.java
│   ├── WordViewModel.java
│   ├── WordListFragment.java       # 列表页（骨架屏加载）
│   └── WordDetailFragment.java     # 详情页（TTS→录音→ISE评分闭环）
│
├── linking/                        # 连读模块（Phase 4）
│   ├── LinkingEntity.java
│   ├── LinkingDao.java
│   ├── LinkingRepository.java
│   ├── LinkingViewModel.java
│   ├── LinkingListFragment.java    # 列表页（ChipGroup分类筛选）
│   └── LinkingDetailFragment.java  # 详情页（波形+评分）
│
├── listening/                      # 听力模块（Phase 5）
│   ├── ListeningAudioEntity.java
│   ├── ListeningQuestionEntity.java
│   ├── ListeningAudioDao.java
│   ├── ListeningViewModel.java
│   ├── ListeningListFragment.java  # 列表页（难度筛选）
│   └── ListeningDetailFragment.java# 详情页（TTS逐句播放+答题批改）
│
├── shadowing/                      # 听力跟读模块（Phase 10）
│   ├── ShadowingListFragment.java  # 列表页（骨架屏加载）
│   └── ShadowingDetailFragment.java# 详情页（逐句导航+全列表TTS链+ISE评分）
│
├── pronunciation/                  # 发音实验室（Phase 9b）
│   ├── PronunciationLabFragment.java # 最小对立体对比发音
│   └── PronunciationViewModel.java
│
├── drill/                          # 句型操练（Phase 9c）
│   ├── PatternDrillFragment.java   # 操练主界面
│   ├── DrillViewModel.java
│   ├── GrammarListAdapter.java
│   └── SummaryAdapter.java
│
├── dialogue/                       # 情景对话（Phase 9d）
│   ├── DialogueFragment.java       # 5阶段状态机
│   ├── DialogueViewModel.java
│   ├── ScenarioAdapter.java
│   ├── DialogueSummaryAdapter.java
│   └── RoleplayAdapter.java
│
├── chat/                           # AI 英语语伴（Phase 8 子模块）
│   ├── ChatFragment.java
│   ├── ChatViewModel.java
│   ├── ChatMessage.java
│   └── ChatAdapter.java
│
├── progress/                       # 学习进度（Phase 6）
│   ├── UserProgressEntity.java
│   ├── UserProgressDao.java
│   ├── UserProgressRepository.java
│   ├── PracticeRecordEntity.java   # 练习记录
│   ├── PracticeRecordDao.java
│   └── ProgressViewModel.java
│
├── settings/                       # 设置（Phase 6）
│   └── SettingsFragment.java       # 统计面板+数据导出重置+TTS状态检测
│
└── core/                           # 公共基础设施
    ├── audio/
    │   ├── TTSEngine.java          # TTS（缓存+语言检测+队列+多引擎回退）
    │   ├── AudioRecorder.java      # 录音（AudioRecord+PCM→WAV）
    │   └── AudioWaveformView.java  # 自定义波形视图（State机器+勾选动画）
    ├── scorer/
    │   ├── Scorer.java             # 评分接口
    │   ├── MockScorer.java         # 模拟评分（调试用）
    │   └── XunfeiScorer.java       # 讯飞 ISE SDK 实现（生产用）
    ├── database/
    │   ├── AppDatabase.java        # Room DB（v1→v2→v3 迁移）
    │   └── Converters.java         # 类型转换器
    ├── net/
    │   ├── ApiConfig.java          # API 配置（MiMo 端点/Key）
    │   └── MiMoApiService.java     # MiMo API（OkHttp + SSE 聊天）
    ├── model/
    │   └── Common.java             # 泛型 Result<T> 封装
    ├── animation/
    │   └── AnimationUtils.java     # 轻量动画工具（缩放淡入、逐项弹入）
    └── util/
        └── UuidManager.java        # 设备 UUID 生成与持久化
```

## 数据流

```
用户点击录音按钮
       ↓
Fragment → ViewModel.startRecording()
       ↓
AudioRecorder.start() → 录音中 → AudioWaveformView 实时绘制
       ↓ (录音完成)
AudioRecorder.stop() → WAV 文件
       ↓
ViewModel.startEvaluation()
       ↓
XunfeiScorer.startEvaluation(audioFile, referenceText)
       ↓ (ISE SDK 异步回调)
ScorerCallback.onResult(score, detail)
       ↓ (线程切换 + isAdded 守卫)
Fragment → AudioWaveformView.showSuccess() + 显示评分
       ↓
Repository.saveRecord() → PracticeRecordDao.insert()
```

## 导航架构

```
MainActivity (AppCompatActivity)
  └── DrawerLayout
       ├── content (LinearLayout)
       │   ├── MaterialToolbar
       │   ├── FragmentContainerView (NavHostFragment)
       │   │   ├── home/LearnFragment           ← startDestination
       │   │   │   └── TabLayout + ChildFragmentManager
       │   │   │       ├── 🌟 单词 (WordTabFragment 内嵌 WordListFragment)
       │   │   │       ├── 🔗 连读 (LinkingTabFragment 内嵌 LinkingListFragment)
       │   │   │       └── 🎧 跟读 (ShadowingTabFragment 内嵌 ShadowingListFragment)
       │   │   ├── home/PracticeRootFragment
       │   │   │   └── TabLayout + ChildFragmentManager
       │   │   │       ├── 🔬 发音 (PronunciationLabFragment)
       │   │   │       ├── 📝 句型 (PatternDrillFragment)
       │   │   │       └── 💬 对话 (DialogueFragment)
       │   │   ├── chat/ChatFragment            (AI 语伴)
       │   │   ├── settings/SettingsFragment
       │   │   ├── home/GuideFragment           (引导页)
       │   │   ├── word/WordListFragment        (列表页)
       │   │   ├── word/WordDetailFragment      (详情页)
       │   │   ├── linking/LinkingListFragment
       │   │   ├── linking/LinkingDetailFragment
       │   │   ├── listening/ListeningListFragment
       │   │   ├── listening/ListeningDetailFragment
       │   │   ├── shadowing/ShadowingListFragment
       │   │   └── shadowing/ShadowingDetailFragment
       │   └── BottomNavigationView
       │       ├── 🏠 学习 → home/LearnFragment
       │       ├── 📖 练习 → home/PracticeRootFragment
       │       └── ⚙️ 设置 → settings/SettingsFragment
       └── NavigationView (侧边抽屉)
           ├── 📖 学习引导 → GuideFragment
           ├── 🤖 AI 语伴 → ChatFragment
           ├── 💪 单词跟读 → WordListFragment
           ├── 🔗 连读练习 → LinkingListFragment
           ├── 👂 听力训练 → ListeningListFragment
           ├── 🎧 听力跟读 → ShadowingListFragment
           └── ... (各模块快捷入口)
```

## 主题系统

- Material 3 XML 主题（浅色 + 深色模式）
- 浅色：`res/values/themes.xml`
- 深色：`res/values-night/themes.xml`
- 主色调：清新蓝（`#1565C0 / Blue700`）
- 自定义 color attributes（`?attr/colorPrimaryContainer` 等），供 drawable 引用
- 圆角统一管理：`res/values/dimens.xml`（corner_small/medium/large/pill）
- 系统设置自动切换深色/浅色

## 关键架构决策

| 决策 | 方案 | 原因 |
|------|------|------|
| 语言 | Java 17 | 避免 Compose/Kotlin 编译环境复杂度 |
| UI | XML Layouts | 同上 + Material Components 文档更稳定 |
| 首页标签页 | TabLayout + ChildFragmentManager | 避免 Navigation Component 嵌套导致的状态混乱 |
| 数据库 | Room + createFromAsset | 数据量大且固定，运行期建表 + 初始化太慢 |
| JSON 模块 | AssetManager 加载 | 发音/句型/情景对话数据更新频繁，免 Room 表迁移 |
| 评分接口 | Scorer 抽象接口 | 开发期 MockScorer 调流程，生产期 XunfeiScorer 真评分 |
| TTS 引擎 | 多引擎回退链 | 国产 ROM（OPPO/Xiaomi/Huawei/Vivo）兼容 |
| 异步回调 | isAdded 守卫 | 防止 Fragment 销毁后崩溃 |
| 波形视图 | 自定义 View（无外部库） | 轻量、完全控制绘制和动画状态 |
