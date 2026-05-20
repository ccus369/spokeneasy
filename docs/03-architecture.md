# SpokenEasy - 架构设计

## 总体架构模式：MVVM + Repository

```
[Fragment/XML UI] → [ViewModel] → [Repository] → [Room Database / TTS / Scorer]
     ↑                  |                |
     └── LiveData ──────┘                └── 数据唯一来源
```

## 分包结构

```
com.spokeneasy.app/
├── SpokenEasyApp.java           # Application
├── MainActivity.java            # 入口Activity (AppCompatActivity)
├── word/                        # 单词板块（功能分包）
│   ├── WordEntity.java
│   ├── WordDao.java
│   ├── WordRepository.java
│   ├── WordViewModel.java
│   ├── WordListFragment.java
│   └── WordDetailFragment.java
├── linking/                     # 连读板块
├── listening/                   # 听力板块
├── settings/                    # 设置
│   └── SettingsFragment.java
├── progress/                    # 学习进度
│   ├── UserProgressEntity.java
│   ├── UserProgressDao.java
│   ├── UserProgressRepository.java
│   └── ProgressViewModel.java
└── core/                        # 公共基础设施
    ├── audio/
    │   ├── TTSEngine.java       # TTS 播放
    │   └── AudioRecorder.java   # 录音
    ├── scorer/
    │   ├── Scorer.java          # 评分接口
    │   ├── MockScorer.java      # 模拟评分
    │   └── XunfeiScorer.java    # 讯飞实现
    ├── database/
    │   └── AppDatabase.java
    ├── model/
    │   └── Common.java
    └── util/
        └── UuidManager.java
```

## 数据流

```
            User Action
                ↓
    ViewModel.onEvent(event)
                ↓
    Repository.execute() ──→ Room DAO
                ↓              ↓
    LiveData<UiState>     SQLite DB
                ↓
    Fragment observes → updates XML UI
```

## 导航架构

```
MainActivity (AppCompatActivity)
  └── DrawerLayout
       ├── LinearLayout (main content)
       │   ├── MaterialToolbar (ActionBar)
       │   ├── FragmentContainerView (NavHostFragment)
       │   │   ├── WordListFragment (start destination)
       │   │   ├── LinkingListFragment
       │   │   └── SettingsFragment
       │   └── BottomNavigationView
       └── NavigationView (side drawer)
```

## 主题系统

- Material 3 XML 主题
- 浅色模式 + 深色模式 (values/themes.xml + values-night/themes.xml)
- 主色调：清新蓝 (#1565C0 / Blue700)
- 自定义 color attributes
- 支持系统设置自动切换
