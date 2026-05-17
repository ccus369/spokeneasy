# SpokenEasy — 项目状态与开发计划

## 当前状态 (2026-05-17)
全部 8 个阶段 + Phase 9 新功能完成，BUILD SUCCESSFUL 验证通过。

| 阶段 | 状态 | 说明 |
|------|------|------|
| Phase 1-6 | ✅ | 基础功能全部就绪 |
| Phase 5.5 | ✅ | UI 改造 + 听力 TTS 重构 |
| Phase 7 | ✅ | 讯飞 ISE 真实语音评测取代 MockScorer |
| Phase 8 | ✅ | 练习记录 + TTS 设置 + AI 英语语伴聊天 |
| Phase 9a | ✅ | 学习首页重构 (TabLayout: 单词/连读) + 练习首页 (TabLayout: 发音/句型/对话) |
| Phase 9b | ✅ | 发音实验室 (最小对立体 minimal pairs 对比发音 + ISE 评分) |
| Phase 9c | ✅ | 句型操练 (4 种题型: 替换/转换/扩展/问答, 多 JSON 题库, 3 阶段: 选择→操练→总结) |
| Phase 9d | ✅ | 情景对话 (场景选择→预热词汇→对话跟读→AI 角色扮演→总结报告) |

### 已完成的转换
- Gradle 配置：移除 Compose/Kotlin 插件，添加 Navigation Fragment/UI、Material Components、AppCompat
- 主题系统：从 Compose Material3 ColorScheme 转换为 XML Material3 主题 (浅色+深色)
- 导航：从 Navigation Compose NavHost 转换为 Navigation Component nav_graph.xml + NavigationUI
- Activity：从 ComponentActivity + setContent {} 转换为 AppCompatActivity + setContentView()
- UI：从 Composable 函数转换为 Fragment + XML layout
- 底部导航：从 NavigationBar + NavigationBarItem 转换为 BottomNavigationView + menu XML
- 抽屉导航：从 ModalNavigationDrawer + ModalDrawerSheet 转换为 DrawerLayout + NavigationView
- 图标：从 Compose Material Icons Vector 转换为 XML Vector Drawable

### 保留不变
- 功能分包结构 (word/linking/settings)
- Room 数据库依赖
- Application 类
- AndroidManifest 配置
- Launcher icon / mipmap 资源

---

## 版本勘误（重要）

`docs/02-technical-spec.md` 中的版本号是**初始计划**，实际已验证可用的版本为：

| 配置项 | 文档写的（别用） | 已验证可用的 |
|--------|----------------|------------|
| Gradle | 9.4.1 | 8.14.5 |
| AGP | 9.2.1 | 8.2.2 |
| Kotlin | 2.0.x | 1.9.22（已移除，转 Java 17） |
| Compose BOM | 2024.x | 2024.02.00（已移除，转 XML） |

**不要用 Gradle 9.x / AGP 9.x**，Gradle 9 移除了部分 API，会导致编译错误。

---

## 给新对话的启动指令

1. 先完整阅读本文件和 CLAUDE.md
2. 再读 docs/ 下 4 份文档
3. 从 Phase 2 开始逐阶段实现
4. 每完成一个模块 → 编译验证 → 再继续

---

## 完整开发计划（6 个阶段）

### Phase 1：项目骨架搭建 ✅
- Gradle 配置（AGP / Room 版本对齐）
- Application + MainActivity (Java)
- XML 主题系统（浅色+深色 Material3）
- 导航框架（DrawerLayout + BottomNavigationView + NavHostFragment）
- 占位 Fragment 页面
- AndroidManifest
- **验证标准：App 能跑起来，导航能切换**

### Phase 2：基础设施 ✅
- 所有 Entity + DAO（word/linking/listening/progress）
- AppDatabase（Room + createFromAsset）
- assets/database/spokeneasy.db（预置数据库文件，含5张表空库）
- core/model/Common.java（UiResult + ScoreResult 公共数据类）
- TTSEngine（TextToSpeech 封装）+ AudioRecorder（录音+播放）
- Scorer 接口 + MockScorer（随机评分 40-100）
- UuidManager（设备 UUID 持久化）
- **验证标准：BUILD SUCCESSFUL，数据库初始化就绪**

### Phase 3：单词板块 ✅
- WordRepository（获取/搜索单词，异步插入/更新）
- WordViewModel（AndroidViewModel + LiveData 管理列表状态）
- WordListFragment 重构：搜索框 + RecyclerView 单词列表
- WordListAdapter（DiffUtil 分页适配器，显示单词+音标）
- WordDetailFragment（单词详情：3 句例句 + TTS 播放 + 中英文切换 + 跟读录音 + MockScore 评分）
- item_word.xml 列表项布局 + fragment_word_detail.xml 详情布局
- 导航：添加 wordDetailFragment destination，wordId 参数传递
- **验证标准：BUILD SUCCESSFUL，单词列表可加载，点击进入详情**

### Phase 4：连读板块 ✅
- LinkingEntity + LinkingDao（标准 CRUD）
- LinkingRepository（获取/搜索连读规则，异步操作）
- LinkingViewModel（AndroidViewModel + LiveData）
- LinkingListFragment + LinkingListAdapter（DiffUtil 列表）
- LinkingDetailFragment（规则说明 + 例句 + TTS 播放 + 跟读录音 + MockScore 评分）
- fragment_linking_list.xml + fragment_linking_detail.xml 布局
- 导航：linkingListFragment → linkingDetailFragment(linkingId:long)
- **验证标准：BUILD SUCCESSFUL，连读列表可加载，详情可录音评分**

### Phase 5：听力板块 ✅
- ListeningAudioEntity + ListeningQuestionEntity（双表外键关联，CASCADE 删除）
- ListeningAudioDao（含关联查询：getQuestionsByAudioId）
- ListeningRepository + ListeningViewModel（AndroidViewModel + LiveData）
- ListeningListFragment + ListeningListAdapter（ChipGroup 三级难度筛选）
- ListeningDetailFragment（对话显示 → 3 道 RadioGroup 选择题 → 提交自动批改 → 对错颜色反馈）
- fragment_listening_list.xml + fragment_listening_detail.xml 布局
- 导航：listeningListFragment → listeningDetailFragment(audioId:long)
- **验证标准：BUILD SUCCESSFUL，听力列表可筛选，答题可批改得分**

### Phase 6：设置 + 统计 ✅
- UserProgressRepository（标准 CRUD + 统计查询）
- UserProgressViewModel（AndroidViewModel，加载各模块统计数据）
- SettingsFragment 完全重写：统计卡片（单词/连读/听力完成数 + ProgressBar）、数据导出（Intent.ACTION_SEND 分享）、数据重置（AlertDialog 确认后清空）
- fragment_settings.xml 完全重写：Material3 卡片布局，设备 UUID 显示
- **验证标准：BUILD SUCCESSFUL，设置页显示各模块统计数据，导出/重置功能正常**

### Phase 7：讯飞 ISE 语音评测 SDK 集成 ✅
- XunfeiScorer 实现 Scorer 接口，对接讯飞 ISE SDK（`com.iflytek.ise`）
- `parseAndCallback()` 解析 ISE 结果：总评分 + 逐词评分（✓/⚠） + 音素级发音建议（13 个音素映射）
- `formatWordScores()`：遍历 Sentence → Syll → Word，根据 `rec_node_type == "noise"` 过滤噪音，得分 ≥60 显示 ✓、<60 显示 ⚠
- `formatPhonemeTips()`：遍历 Word → Phone，当 `dp_message` 包含 `"Miss"`/`"Add"`/`"Sub"`/`"Mis"` 时输出错误描述，得分 <60 时输出参考级建议
- `getPhonemeTip()`：13 个音素代码 → 中文发音建议（th→/θ/ 咬舌、dh→/ð/ 咬舌振动、r→/r/ 卷舌、v→/v/ 上牙碰下唇等）
- AudioWaveformView：Canvas 自定义 View，实时绘制录音波形（FFT 风格竖条）
- TTSEngine 增强：缓存（`HashMap<String, String>`）、语言自动检测（isChinese）、发音队列（speakQueue + isSpeaking flag）、seekBar 进度同步
- **验证标准：BUILD SUCCESSFUL，跟读/连读评分显示真实 ISE 评分（非模拟），波形可视化**

### Phase 8：练习记录 + 系统设置 + AI 聊天 ✅

**练习记录与历史回放（Phase 8a）：**
- PracticeRecordEntity（Room @Entity，字段：id/userUuid/moduleType/itemId/referenceText/score/detail/audioFilePath/createdAt）
- PracticeRecordDao（getAll DESC、getRecent DESC+LIMIT、getByModule DESC、insert REPLACE）
- PracticeRecordRepository + RecordHistoryViewModel（AndroidViewModel）
- RecordHistoryFragment：RecyclerView + HistoryAdapter，moduleBadge 彩色方块（word=蓝/#1976D2、linking=绿/#43A047、listening=橙/#E65100），score 颜色分级（≥80 绿/#43A047、≥60 橙/#F57C00、<60 红/#E53935），参考文本截断 50 字，MediaPlayer 音频回放
- DB Migration MIGRATION_1_2：CREATE TABLE practice_records + 2 索引（user_uuid、created_at），替换 fallbackToDestructiveMigration()
- 在 WordDetailFragment 和 LinkingDetailFragment 的 ISE 评分回调中调用 savePracticeRecord()

**TTS 设置面板与状态检测（Phase 8b）：**
- TtsHelper：一次性的 TextToSpeech 引擎状态检测，TtsCheckCallback 返回 4 种状态（AVAILABLE/MISSING_DATA/NOT_SUPPORTED/NO_ENGINE）
- **多引擎回退链**（国产手机兼容）：默认引擎失败 → `TextToSpeech.getEngines()` 逐一遍历 → `Settings.Secure.tts_default_synth` → 已知 OEM 引擎包名硬编码列表（OPPO `com.oplus.ttsaccessibilityengine`、Xiaomi `com.xiaomi.tts`、Huawei `com.huawei.tts`、Vivo `com.vivo.tts`）
- **同步 OnInitListener 处理**：国产 ROM 上 listener 在构造器中同步触发，用 `boolean[] listenerFired + int[] initStatus` 数组模式确保引用安全
- **引擎信息显示**：SettingsFragment 显示当前引擎名，NO_ENGINE 时列出所有已安装引擎 + "重试检测"按钮 + 音量检测
- AndroidManifest 添加 `<queries>` TTS_SERVICE intent 声明（Android 11+ 包可见性）
- SettingsFragment TTS 面板：状态显示 + Test（TTSEngine 播放测试）、Settings（`com.android.settings.TTS_SETTINGS`）、Install（`ACTION_INSTALL_TTS_DATA` 或 Play Store `com.google.android.tts`）
- feedback_bg shape drawable（8dp 圆角矩形）+ dark theme colors.xml（feedback_bg_color=#0FFFFFFF）
- 评分显示格式优化：移除"总分:"前缀，直接显示"85 分"
- 讯飞 ISE 评分反馈增强：逐词 ✓/⚠ + 音素级发音建议

**AI 英语对话聊天模块（Phase 8c）：**
- MiMoApiService（OkHttp 4.12.0）：调用 `api.xiaomimimo.com/v1/chat/completions`（OpenAI 兼容），模型 `mimo-v2-flash`，30s 超时，Bearer token 认证
- ApiConfig：SharedPreferences 封装，存取 MiMo API Key
- 6 条 System Prompt 规则：英语语伴，2-4 句回复，纠错格式 `\n---\n📝 Correction` + `💡 More natural`，中文翻译，自适应难度
- ChatMessage：Role 枚举（USER/ASSISTANT/SYSTEM），`parseCorrections()` 按 `\n---` 分隔为 replyText + correctionContent
- ChatViewModel（AndroidViewModel）：`sendText()` 在 `databaseWriteExecutor` 后台线程调用 API，LiveData 驱动 UI，历史窗口 20 条
- ChatAdapter：2 ViewType（TYPE_USER 蓝色右对齐气泡 / TYPE_AI 灰色左对齐气泡 + 纠错卡片 + TTS 播放按钮）
- ChatFragment（ConstraintLayout）：toolbar（返回/清空/设置）→ ProgressBar → RecyclerView → API Key 缺失横幅 → 输入栏（MaterialCardView EditText + 发送按钮），Enter 发送/Shift+Enter 换行
- SettingsFragment 集成：MiMo API Key TextInputLayout（密码模式） + Save 按钮 + Snackbar
- 导航集成：nav_graph.xml chatFragment destination + drawer_menu.xml chat 菜单项
- 依赖：libs.versions.toml okhttp=4.12.0 + app/build.gradle.kts implementation(libs.okhttp)
- **验证标准：BUILD SUCCESSFUL，AI 对话可发送/接收，纠错卡片可展开，TTS 可朗读，API Key 可在设置页配置**

---

## Phase 9：学习首页 + 练习模块 (Phase 9a-9d) ✅

### Phase 9a：首页重构（学习 + 练习）
- **LearnFragment**：TabLayout 双标签容器，Tab 0 → WordListFragment（单词学习），Tab 1 → LinkingListFragment（连读练习）
- **PracticeRootFragment**：TabLayout 三标签容器，Tab 0 → PronunciationLabFragment（发音实验室），Tab 1 → PatternDrillFragment（句型操练），Tab 2 → DialogueFragment（情景对话）
- **导航重构**：底部导航改为"学习"和"练习"两项，去掉独立的"听力练习"菜单项（听力移至后续扩展）
- **DrawerLayout** 同步更新：菜单项与底部导航保持一致

### Phase 9b：发音实验室 (Pronunciation Lab)
- **最小对立体 (Minimal Pairs)**：从 `assets/pronunciation/minimal_pairs.json` 加载音素对比数据
- **音素分类筛选**：ChipGroup 按音素类别筛选（如 /iː/ vs /ɪ/、/θ/ vs /ð/）
- **对比发音**：每个 pair 显示 wordA/wordB + 音标 + 例句，TTS 播放参考发音
- **跟读录音 + ISE 评分**：录音后自动调用 XunfeiScorer 评分，结果保存到 PracticeRecord
- **发音技巧提示**：tipCn 字段显示中文发音要点（如"上齿咬下唇"）

### Phase 9c：句型操练 (Pattern Drill)
- **4 种题型**：替换练习 (SUBSTITUTION)、转换练习 (TRANSFORMATION)、扩展练习 (EXPANSION)、问答练习 (RESPONSE)
- **多重题库**：从 `assets/drills/*.json` 加载 6 个语法点（一般现在时、现在进行时、一般过去时、be 动词、there be 句型、情态动词），每个语法点包含多种题型
- **3 阶段流程**：SELECTING（选择语法点）→ DRILLING（逐题操练 + TTS 播放 + 录音 + ISE 评分）→ SUMMARY（成绩汇总 + 答题详情）
- **高亮标记**：base 句中使用 `[括号]` 标记替换部分，显示为琥珀色高亮
- **提示系统**：cue 显示替换提示，hintCn 提供中文辅助提示

### Phase 9d：情景对话 (Scenario Dialogue)
- **场景加载**：从 `assets/dialogues/scenarios.json` 加载多场景对话数据
- **5 阶段流程**：SCENE_SELECT（选择场景）→ WARMUP（预热词汇 + TTS 播放）→ DIALOGUE（逐句跟读 + 录音 + ISE 评分 + 导航）→ ROLEPLAY（AI 角色扮演，MiMo API 驱动）→ SUMMARY（成绩汇总 + 重点句型）
- **对话跟读**：Speaker A/B 角色标识，逐句录音评分，支持前后句导航
- **AI 角色扮演**：集成 MiMo API（`mimo-v2-flash`），场景 system prompt 驱动自由对话，纠错反馈
- **评分与总结**：每句独立 ISE 评分，总结页显示平均分 + 各句成绩 + 场景重点句型

---

## Build 历程总结（已踩过的坑，避免再踩）

| 问题 | 原因 | 正确做法 |
|------|------|---------|
| `NoSuchMethodError: module(Object)` | Gradle 9.x 移除了 API | 用 Gradle 8.14.5 + AGP 8.2.2 |
| 插件找不到 `com.android.application` | settings.gradle.kts 的 google() 加了 content 过滤 | 去掉 content 过滤 |
| Gradle 下载超时 | 国内网络问题 | 用腾讯云镜像下载，手动放入缓存 |
| SHA256 校验失败 | 用了错误版本的 checksum | 版本确定后从 Gradle 官网获取正确 checksum |
| Theme.Material3.DayNight.NoActionBar 找不到 | 缺少 `com.google.android.material:material` 依赖 | 添加 Material Components 依赖（已在 Java 版本中添加） |
