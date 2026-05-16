# SpokenEasy - 技术规范

## 开发环境
- Android Studio Ladybug / Koala
- Gradle 9.4.1
- AGP 9.2.1
- JDK 21 (toolchain)
- Kotlin 2.0.x
- Compose BOM 2024.x

## 依赖管理
使用版本目录 (libs.versions.toml) 统一管理依赖版本。

### 核心依赖
| 库 | 版本 | 用途 |
|----|------|------|
| Kotlin | 2.0.x | 编程语言 |
| Compose BOM | 2024.02.00+ | Compose UI |
| Material 3 | 跟随BOM | UI组件 |
| Room | 2.6.1 | 本地数据库 |
| Navigation Compose | 2.7.x | 页面导航 |
| Lifecycle | 2.7.0 | ViewModel |
| Activity Compose | 1.8.0 | Activity集成 |

## 代码规范

### Kotlin
- 使用 Kotlin 官方编码规范
- 文件命名：PascalCase，与类名一致
- 包名全小写，按功能分包

### Compose
- Screen 文件统一后缀：`*Screen.kt`
- ViewModel 文件统一后缀：`*ViewModel.kt`
- 遵循 Compose 准则：状态向下，事件向上
- ViewModel 使用 `viewModel()` 默认工厂

### 资源命名
- drawable: `ic_*` 前缀
- string: 全小写蛇形
- color: 全小写蛇形

## 版本策略
- versionCode: 递增整数
- versionName: 语义化版本 (1.0.0)
- 每次发布更新 versionCode

## 构建类型
- debug: 可调试，不混淆
- release: 混淆，压缩
