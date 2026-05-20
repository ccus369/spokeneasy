# SpokenEasy - 技术规范

## 开发环境
- Android Studio Ladybug / Koala
- Gradle 8.14.5
- AGP 8.2.2
- JDK 17
- Java 17 (source/target compatibility)

## 依赖管理
使用版本目录 (libs.versions.toml) 统一管理依赖版本。

### 核心依赖
| 库 | 版本 | 用途 |
|----|------|------|
| Java | 17 | 编程语言 |
| Material Components | 1.11.0 | UI组件 (M3主题) |
| Room | 2.6.1 | 本地数据库 |
| Navigation Fragment | 2.7.7 | 页面导航 |
| Navigation UI | 2.7.7 | NavigationUI自动同步 |
| AppCompat | 1.6.1 | Activity/Fragment基础 |
| Lifecycle | 2.7.0 | ViewModel |

## 代码规范

### Java
- 使用 Java 17 官方编码规范
- 文件命名：PascalCase，与类名一致
- 包名全小写，按功能分包

### Fragment/XML
- Fragment 文件统一后缀：`*Fragment.java`
- ViewModel 文件统一后缀：`*ViewModel.java`
- 遵循 MVVM 准则：状态向下（LiveData），事件向上（View.onClickListener）
- ViewModel 使用 `new ViewModelProvider(this).get()` 获取

### 资源命名
- drawable: `ic_*` 前缀
- string: 全小写蛇形
- color: 全小写蛇形
- layout: `activity_*.xml` / `fragment_*.xml`
- menu: `menu/*.xml`
- navigation: `navigation/*.xml`

## 版本策略
- versionCode: 递增整数
- versionName: 语义化版本 (1.0.0)
- 每次发布更新 versionCode

## 构建类型
- debug: 可调试，不混淆
- release: 混淆，压缩
