# SpokenEasy - 技术规范

> 📅 最后更新：2026-06-01

## 开发环境
- Android Studio Ladybug / Koala
- Gradle 8.14.5
- AGP 8.2.2
- JDK 17（C:/DDD/Android/jbr/）

## 编程语言
- Java 17（source/target compatibility）
- ⚠️ **不使用 Kotlin / Jetpack Compose** — 早期尝试后全面回退 Java + XML

## 依赖管理
使用版本目录 `gradle/libs.versions.toml` 统一管理依赖版本。

### 核心依赖
| 库 | 版本 | 用途 |
|----|------|------|
| Java | 17 | 编程语言 |
| Material Components | 1.11.0 | UI 组件（M3 主题） |
| Room Runtime | 2.6.1 | 本地数据库 |
| Room Compiler | 2.6.1 | Room 注解处理器（annotationProcessor） |
| Navigation Fragment | 2.7.7 | 页面导航 |
| Navigation UI | 2.7.7 | NavigationUI 自动同步 BottomNav |
| AppCompat | 1.6.1 | Activity/Fragment 基础 |
| Lifecycle Runtime | 2.7.0 | LiveData / LifecycleOwner |
| Lifecycle ViewModel | 2.7.0 | ViewModel 组件 |
| ConstraintLayout | 2.1.4 | 复杂布局 |
| Core KTX | 1.12.0 | AndroidX 核心（保留兼容） |
| Activity | 1.8.2 | Activity 1.8+ API |
| OkHttp | 4.12.0 | HTTP 请求（MiMo API 聊天 + MockWebServer 测试） |
| JSON (org.json) | 20231013 | JSON 数据解析（数据驱动模块） |

### 测试依赖
| 库 | 版本 | 用途 |
|----|------|------|
| JUnit | 4.13.2 | 单元测试 |
| OkHttp MockWebServer | 4.12.0 | HTTP mock 测试 |
| AndroidX Test JUnit | 1.1.5 | Instrumented 测试 |
| Espresso Core | 3.5.1 | UI 自动化测试 |

### ⚠️ 版本勘误
以下版本在文档中是**初始计划**，不要使用：

| 配置项 | ❌ 文档旧值（别用） | ✅ 实际可用值 |
|--------|-------------------|-------------|
| Gradle | 9.4.1 | 8.14.5 |
| AGP | 9.2.1 | 8.2.2 |
| Kotlin | 2.0.x | 已移除，转 Java 17 |
| Compose BOM | 2024.x | 已移除，转 XML |

## 代码规范

### Java
- 使用 Java 17 官方编码规范
- 文件命名：PascalCase，与类名一致
- 包名全小写，按功能分包

### 命名约定
- Fragment 文件后缀：`*Fragment.java`
- ViewModel 文件后缀：`*ViewModel.java`
- Adapter 文件后缀：`*Adapter.java`
- 布局文件：`activity_*.xml` / `fragment_*.xml` / `item_*.xml`
- drawable 资源：`ic_*` 前缀（图标）、`bg_*` 前缀（背景）
- String 资源：全小写蛇形
- Color 资源：全小写蛇形
- 菜单文件：`menu/*.xml`
- 导航文件：`navigation/nav_graph.xml`
- 网络服务：`core/net/*Service.java`
- 网络配置：`core/net/*Config.java`

### MVVM 准则
- 状态向下（LiveData），事件向上（View.onClickListener）
- Fragment 不直接操作数据，通过 ViewModel 获取
- ViewModel 使用 `new ViewModelProvider(this).get()` 获取
- Repository 是数据唯一来源，屏蔽 DAO / 网络 / JSON 差异
- 异步回调入口必须加 `isAdded()` / `binding != null` 守卫

### 资源限定符
- `res/values/` — 浅色模式
- `res/values-night/` — 深色模式（系统自动切换）
- 代码中统一使用 `ContextCompat.getColor(context, R.color.xxx)` 获取颜色

## 版本策略
- versionCode: 递增整数（当前 5+）
- versionName: 语义化版本（当前 1.0.0）
- 每次发布更新 versionCode

## 构建类型
- debug: 可调试，不混淆，adb install -r 直接安装
- release: 混淆，压缩（暂未使用）
- 构建命令：`export JAVA_HOME=/c/DDD/Android/jbr/ && ./gradlew assembleDebug`
