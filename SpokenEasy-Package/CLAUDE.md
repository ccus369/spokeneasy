# SpokenEasy - CLAUDE 工作指引

## 项目概述
Android 英语口语练习 App "SpokenEasy"，Kotlin + Jetpack Compose + Room。

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

### 代码规范
- Kotlin + Compose
- MVVM 模式：Screen → ViewModel → Repository → DAO
- 状态向下，事件向上
- Compose 文件后缀 `*Screen.kt`
- ViewModel 文件后缀 `*ViewModel.kt`

### 关键架构决策
- 预置 .db 文件：`assets/database/spokeneasy.db`（SQLite 可视化工具创建）
- 导航：DrawerLayout + BottomNavigationBar
- 评分：Scorer 接口 → MockScorer(模拟) / XunfeiScorer(讯飞)
- 音频：TTSEngine + AudioRecorder 独立封装
- 主题：浅色+深色模式，蓝色主色调
- 用户：设备UUID标识，仅游客模式

### Phase 进度（新项目重写）
- 从 Phase 1 开始重新构建