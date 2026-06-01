# SpokenEasy - 英语口语练习APP 需求文档

> 📅 最后更新：2026-06-01

## 项目概述
开发一款安卓英语口语练习 APP，包含多个学习模块：单词跟读、连读规则、听力训练、发音实验室、句型操练、情景对话、听力跟读（Shadowing）、AI 英语聊天。

## 技术栈（实际采用）
| 项目 | 选择 |
|------|------|
| 语言 | **Java 17**（初期试过 Kotlin，已全面回退） |
| UI 框架 | **XML Layouts + Material Components**（已移除 Jetpack Compose） |
| 数据库 | Room 2.6.1 (SQLite) + 预置 .db 文件（assets/database/spokeneasy.db） |
| 导航 | DrawerLayout + BottomNavigationView + NavHostFragment + TabLayout + ChildFragmentManager |
| 音频合成 | Android TTS（多引擎回退链兼容国产 ROM） |
| 录音 | **AudioRecord + PCM→WAV**（已从 MediaRecorder 迁移） |
| 语音评测 | 讯飞 Android SDK (ISE)，Scorer 接口抽象（MockScorer 占位 → XunfeiScorer 生产） |
| 波形可视化 | 自定义 View AudioWaveformView（实时绘制 + 成功态勾选动画） |
| AI 聊天 | MiMo API（OpenAI 兼容），OkHttp 4.12.0 |
| 最低 SDK | 27 (Android 9.0) |
| 目标 SDK | 36 (Android 16) |
| 包名 | com.spokeneasy.app |
| 应用名 | SpokenEasy |

## 功能模块

### 模块 1：单词跟读（Phase 3 ✅）
- 500 个高频单词，每个包含：
  - 单词本身、音标
  - 3 个常用口语句子（英文 + 中文翻译）
- 功能点：
  - MaterialCardView 列表展示
  - 点击进入详情页查看关联句子
  - 支持隐藏/显示中文翻译
  - 点击句子 TTS 播放发音
  - 跟读录音 + ISE 评分（实时波形可视化）
  - **完成标准：评分 ≥ 60 分**

### 模块 2：口语连读练习（Phase 4 ✅）
- 5 类 42 条连读规则，每条包含：
  - 规则名称、原始写法（如 "give me"）、连读写法（如 "gimme"）、例句
- 连读类别：
  - wanna/gonna / outta / gimme / lemme / dunno / kinda / 连读中 T/D 省略等
- 功能点：
  - 规则分类 ChipGroup 筛选展示
  - 点击播放标准发音（TTS + 波形可视化）
  - 跟读录音 + ISE 评分
  - **完成标准：评分 ≥ 60 分**
  - 新增 ai 字段支持音标注音

### 模块 3：分级听力训练（Phase 5 ✅）
- 3 个难度等级（初级/中级/高级），共 50 段对话
- 每段对话包含：标题、完整对话文本、音频文件（TTS 占位）、3 道选择题
- 功能点：
  - 按难度 ChipGroup 筛选
  - TTS 逐句播放 + 隐藏/显示原文
  - 答题并自动批改（RadioGroup）
  - 记录完成状态
  - **完成标准：答题 ≥ 60 分**

### 模块 4：发音实验室（Phase 9b ✅）
- 最小对立体（minimal pairs）训练
- JSON 驱动数据（minimal_pairs.json），按 6 个音素分类
- 功能点：
  - ChipGroup 音素分类筛选
  - 对比发音 + ISE 评分
  - 准确率统计

### 模块 5：句型操练（Phase 9c ✅）
- 4 类题型：substitution / transformation / expansion / response
- 6 个语法点 JSON 题库（DrillCollection → DrillSet → DrillStep 三层嵌套结构）
- 功能点：
  - 3 阶段流程：选择语法点 → 逐题操练 → 总结报告
  - 每题显示 cue（提示）→ 用户发音 → ISE 评分

### 模块 6：情景对话（Phase 9d ✅）
- 五阶段状态机：场景选择 → 预热词汇 → 跟读专练 → AI 角色扮演 → 总结报告
- 角色扮演基于 MiMo API（OpenAI 兼容）进行 AI 对话
- 跟读环节复用 ISE 评分

### 模块 7：听力跟读 Shadowing（Phase 10 ✅）
- listening.json 数据驱动
- 逐句导航（当前句高亮 + 自动滚动）
- 全列表 TTS 回调链串联播放
- 短句级 ISE 评分
- 总评分卡片动画弹入

### 模块 8：AI 英语语伴（Phase 8 子模块 ✅）
- MiMo API（api.xiaomimimo.com/v1/chat/completions）
- ChatFragment + ChatAdapter + ChatViewModel
- 消息列表展示（用户/ AI 气泡）
- 线程安全回调处理

## 用户系统
- 仅游客模式，设备 UUID 标识（UuidManager）
- 记录：UserProgress（进度）+ PracticeRecord（练习记录，含音频文件路径 + 评分 + 详情）

## 数据来源
- 基础数据由开发者人工整理，预置 .db 文件（SQLite 可视化工具创建）
- 发音 / 句型 / 情景对话 / Shadowing 数据均为 JSON 文件（assets/data/）
- AI 聊天由 MiMo API 实时生成

## 录音 + AI 评分流程
1. 用户点击跟读/录音按钮
2. 录制用户声音（AudioRecord → PCM → WAV，3-10 秒）
3. 调用 Scorer 接口（抽象层，实际 XunfeiScorer 调用 ISE SDK）
4. 返回评分（0-100）和音素级反馈
5. App 展示评分 + 波形动画（成功态勾选）

## 导航结构
```
MainActivity (AppCompatActivity)
  └── DrawerLayout
       ├── LinearLayout (main content)
       │   ├── MaterialToolbar (ActionBar)
       │   ├── FragmentContainerView (NavHostFragment)
       │   │   ├── LearnFragment (首页)
       │   │   │   └── TabLayout + ChildFragmentManager
       │   │   │       ├── WordTabFragment
       │   │   │       ├── LinkingTabFragment
       │   │   │       └── ShadowingTabFragment
       │   │   ├── PracticeRootFragment (练习页)
       │   │   │   └── TabLayout + ChildFragmentManager
       │   │   │       ├── PronunciationTabFragment
       │   │   │       ├── DrillTabFragment
       │   │   │       └── DialogueTabFragment
       │   │   ├── ChatFragment (AI 语伴)
       │   │   ├── SettingsFragment
       │   │   ├── GuideFragment (引导页)
       │   │   ├── word/* (单词详情等)
       │   │   ├── linking/*
       │   │   ├── listening/*
       │   │   └── ... (各模块详情页)
       │   └── BottomNavigationView (3 项)
       └── NavigationView (侧边抽屉)
```

## 完整 Phase 开发进度
| Phase | 内容 | 状态 |
|-------|------|------|
| 1 | 项目骨架搭建（Gradle + 导航框架 + 主题） | ✅ |
| 2 | 基础设施（Entity/DAO/AppDatabase/TTS/录音/Scorer） | ✅ |
| 3 | 单词板块（完整跟读闭环） | ✅ |
| 4 | 连读板块（规则 + 例句 + 跟读） | ✅ |
| 5 | 听力板块（对话 + 答题批改） | ✅ |
| 5.5 | UI 全面改造 + 微动画系统 | ✅ |
| 6 | 设置 + 统计模块 | ✅ |
| 7 | 讯飞 ISE SDK + 波形视图 AudioWaveformView | ✅ |
| 8 | 练习记录 + TTS 多引擎回退 + AI 聊天 | ✅ |
| 9a | 首页 TabLayout 重构（LearnFragment + PracticeRootFragment） | ✅ |
| 9b | 发音实验室（最小对立体） | ✅ |
| 9c | 句型操练（4 类题型） | ✅ |
| 9d | 情景对话（5 阶段状态机 + AI 角色扮演） | ✅ |
| 10 | 听力跟读 Shadowing | ✅ |

## 关键设计决策
- Java + XML 而非 Kotlin + Compose（避免编译环境复杂度，降低新手维护成本）
- 预置 .db 文件（createFromAsset）而非运行期建表 + 初始化
- TTS 多引擎回退链兼容国产 ROM（boolean[] listenerFired + int[] initStatus 数组模式防竞争）
- JSON 数据驱动（发音/句型/情景对话 避免 Room 表膨胀）
- Scorer 接口抽象（开发期 MockScorer，生产期 XunfeiScorer，均不依赖 Activity）
