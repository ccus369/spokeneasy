# SpokenEasy - 架构设计

## 总体架构模式：MVVM + Repository

```
[Compose UI] → [ViewModel] → [Repository] → [Room Database / TTS / Scorer]
     ↑              |                |
     └── State ─────┘                └── 数据唯一来源
```

## 分包结构

```
com.spokeneasy.app/
├── SpokenEasyApp.kt           # Application
├── MainActivity.kt            # 入口Activity
├── navigation/                # 导航
│   ├── BottomNavBar.kt
│   ├── DrawerMenu.kt
│   └── NavGraph.kt
├── word/                      # 单词板块（功能分包）
│   ├── WordEntity.kt
│   ├── WordDao.kt
│   ├── WordRepository.kt
│   ├── WordViewModel.kt
│   ├── WordListScreen.kt
│   └── WordDetailScreen.kt
├── linking/                   # 连读板块
├── listening/                 # 听力板块
├── settings/                  # 设置
│   └── SettingsScreen.kt
├── progress/                  # 学习进度
│   ├── UserProgressEntity.kt
│   ├── UserProgressDao.kt
│   ├── UserProgressRepository.kt
│   └── ProgressViewModel.kt
└── core/                      # 公共基础设施
    ├── audio/
    │   ├── TTSEngine.kt       # TTS 播放
    │   └── AudioRecorder.kt   # 录音
    ├── scorer/
    │   ├── Scorer.kt          # 评分接口
    │   ├── MockScorer.kt      # 模拟评分
    │   └── XunfeiScorer.kt    # 讯飞实现
    ├── database/
    │   └── AppDatabase.kt
    ├── theme/
    │   ├── Color.kt
    │   ├── Type.kt
    │   └── Theme.kt
    ├── model/
    │   └── Common.kt
    └── util/
        └── UuidManager.kt
```

## 数据流

```
            User Action
                ↓
    ViewModel.onEvent(event)
                ↓
    Repository.execute() ──→ Room DAO
                ↓              ↓
    StateFlow<UiState>    SQLite DB
                ↓
    Compose UI recompose
```

## 导航架构

```
MainActivity
  └── ModalDrawerSheet (侧边抽屉)
       └── Scaffold
            └── BottomNavigationBar
                 ├── 主页 (单词板块)
                 ├── 阅读 (连读板块)
                 └── 设置
```

## 主题系统

- Material 3 Dynamic Color
- 浅色模式 + 深色模式
- 主色调：清新蓝 (#1976D2)
- 自定义 ColorScheme
- 支持系统设置自动切换
