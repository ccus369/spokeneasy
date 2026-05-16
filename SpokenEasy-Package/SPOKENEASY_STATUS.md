# SpokenEasy — 项目状态与开发计划

## 当前状态 (2026-05-16)
全部 6 个阶段 + Phase 5.5 UI 改造已完成，BUILD SUCCESSFUL 验证通过。

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

---

## Build 历程总结（已踩过的坑，避免再踩）

| 问题 | 原因 | 正确做法 |
|------|------|---------|
| `NoSuchMethodError: module(Object)` | Gradle 9.x 移除了 API | 用 Gradle 8.14.5 + AGP 8.2.2 |
| 插件找不到 `com.android.application` | settings.gradle.kts 的 google() 加了 content 过滤 | 去掉 content 过滤 |
| Gradle 下载超时 | 国内网络问题 | 用腾讯云镜像下载，手动放入缓存 |
| SHA256 校验失败 | 用了错误版本的 checksum | 版本确定后从 Gradle 官网获取正确 checksum |
| Theme.Material3.DayNight.NoActionBar 找不到 | 缺少 `com.google.android.material:material` 依赖 | 添加 Material Components 依赖（已在 Java 版本中添加） |
