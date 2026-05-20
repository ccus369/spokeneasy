# SpokenEasy

Android 英语口语练习 App，基于 FSI (Foreign Service Institute) 语言学习法，提供递进式口语训练。

## 功能

- **单词学习** — 单词 + 例句 + TTS 跟读 + 讯飞 ISE 语音评分
- **连读规则** — 连读规则讲解 + 对应例句练习
- **发音实验室** — 最小对立体（minimal pairs）对比发音，针对性纠音
- **句型操练** — 替换/转换/扩展/问答四种 Drill 题型，6 个语法点
- **情景对话** — 预热词汇 → 示范跟读 → AI 角色扮演（MiMo API）→ 评估报告
- **AI 英语语伴** — MiMo API 驱动的自由对话
- **学习统计** — 各模块进度追踪，数据导出

## 技术栈

| 层面 | 技术 |
|------|------|
| 语言 | Java 17 |
| UI | XML Layouts + Material 3 |
| 架构 | MVVM (Fragment → ViewModel → Repository → DAO) |
| 导航 | Navigation Component + BottomNavigation + DrawerLayout |
| 数据库 | Room (SQLite) + 预置 assets 数据库 |
| 语音合成 | Android TTS (多引擎回退链，兼容国产 ROM) |
| 语音评测 | 讯飞 ISE SDK (Msc.jar) |
| 网络 | OkHttp 4.12 |
| AI 对话 | MiMo API（OpenAI 兼容） |

## 构建要求

- Android Studio Hedgehog (2023.1.1) 或更新版本
- JDK 17
- Gradle 8.14.5 + AGP 8.2.2
- Android SDK 34 (compileSdk), minSdk 28

## 快速开始

```bash
# 克隆仓库
git clone https://github.com/ccus369/Spokeneasy.git
cd Spokeneasy

# 下载依赖文件（Msc.jar + 预置数据库）
# 方式一：从 GitHub Releases 下载
# 方式二：将讯飞 MSC SDK 的 Msc.jar 放入 app/libs/
# 方式三：手动准备数据库（详见下方说明）
```

### 依赖文件说明

| 文件 | 路径 | 说明 | 获取方式 |
|------|------|------|---------|
| `Msc.jar` | `app/libs/` | 讯飞语音评测 SDK | 从讯飞开放平台下载，或从 Releases 获取 |
| `spokeneasy.db` | `app/src/main/assets/database/` | 预置单词/例句数据库 | 从 Releases 获取 |

## 构建

```bash
./gradlew assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

## 开源协议

MIT License — 详见 [LICENSE](LICENSE)
