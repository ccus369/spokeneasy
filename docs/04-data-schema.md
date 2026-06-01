# SpokenEasy - 数据库设计

> 📅 最后更新：2026-06-01

## 使用方式
- **预置 .db 文件** → `assets/database/spokeneasy.db`
- Room 通过 `createFromAsset()` 加载
- 首次安装：全量校验 Room Entity Schema 与预置 .db 表结构一致性
- 升级安装：通过 Migration 迁移，不校验预置 .db

## 表结构

### words — 单词表（Phase 2）
```sql
CREATE TABLE words (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word TEXT NOT NULL,              -- 单词
    phonetic TEXT,                   -- 音标
    sentence1_en TEXT,               -- 例句1 英文
    sentence1_cn TEXT,               -- 例句1 中文
    sentence2_en TEXT,               -- 例句2 英文
    sentence2_cn TEXT,               -- 例句2 中文
    sentence3_en TEXT,               -- 例句3 英文
    sentence3_cn TEXT,               -- 例句3 中文
    category TEXT                     -- 分类（可选）
);
```
- Room Entity: `WordEntity` → `word/WordEntity.java`
- DAO: `WordDao` → `word/WordDao.java`

### linking — 连读表（Phase 2，Phase 4 扩展）
```sql
CREATE TABLE linking (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    rule_name TEXT NOT NULL,          -- 规则名称
    original TEXT NOT NULL,           -- 原始写法 (e.g. "give me")
    linking_text TEXT NOT NULL,       -- 连读写法 (e.g. "gimme")
    example_en TEXT,                  -- 例句英文
    example_cn TEXT,                  -- 例句中文
    category TEXT,                    -- 分类
    ipa TEXT                          -- 音标（v2→v3 迁移新增）
);
```
- Room Entity: `LinkingEntity` → `linking/LinkingEntity.java`
- DAO: `LinkingDao` → `linking/LinkingDao.java`

### listening_audios — 听力对话表（Phase 2）
```sql
CREATE TABLE listening_audios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,              -- 对话标题
    level INTEGER NOT NULL,           -- 难度等级 (1/2/3)
    dialog_text TEXT NOT NULL,        -- 完整对话文本
    audio_file_name TEXT              -- 音频文件名（预留）
);
```
- Room Entity: `ListeningAudioEntity` → `listening/ListeningAudioEntity.java`
- DAO: `ListeningAudioDao`

### listening_questions — 听力题目表（Phase 2）
```sql
CREATE TABLE listening_questions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    audio_id INTEGER NOT NULL,        -- 关联对话ID
    question TEXT NOT NULL,           -- 题目
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    correct_answer TEXT NOT NULL,     -- 正确答案 (A/B/C)
    FOREIGN KEY (audio_id) REFERENCES listening_audios(id)
);
```
- Room Entity: `ListeningQuestionEntity` → `listening/ListeningQuestionEntity.java`

### user_progress — 用户进度表（Phase 2）
```sql
CREATE TABLE user_progress (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_uuid TEXT NOT NULL,          -- 设备UUID
    module_type TEXT NOT NULL,        -- 模块类型: word/linking/listening
    item_id INTEGER NOT NULL,         -- 对应模块的条目ID
    score INTEGER,                    -- 评分 (0-100)
    is_completed INTEGER DEFAULT 0,   -- 是否完成
    completed_at TIMESTAMP,           -- 完成时间
    UNIQUE(user_uuid, module_type, item_id)
);
```
- Room Entity: `UserProgressEntity` → `progress/UserProgressEntity.java`
- DAO: `UserProgressDao`

### practice_records — 练习记录表（v1→v2 迁移新增，Phase 8）
```sql
CREATE TABLE practice_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    user_uuid TEXT NOT NULL,           -- 设备UUID
    module_type TEXT NOT NULL,         -- 模块类型
    item_id INTEGER NOT NULL DEFAULT 0,-- 对应条目ID
    reference_text TEXT,               -- 参考文本（用户读的那句话）
    score INTEGER NOT NULL DEFAULT 0,  -- 评分 (0-100)
    detail TEXT,                       -- 评分详情（JSON 格式，含音素级反馈）
    audio_file_path TEXT,              -- 录音文件路径
    created_at INTEGER NOT NULL        -- 创建时间戳（毫秒）
);
CREATE INDEX index_practice_records_user_uuid ON practice_records(user_uuid);
CREATE INDEX index_practice_records_created_at ON practice_records(created_at);
```
- Room Entity: `PracticeRecordEntity` → `progress/PracticeRecordEntity.java`
- DAO: `PracticeRecordDao`
- 索引：user_uuid（按用户查询）+ created_at（按时间排序 / 历史回放）

## JSON 数据驱动模块（不占用 Room 表）

以下模块的数据完全由 JSON 文件驱动，通过 `AssetManager` 加载，不创建 Room Entity：

| 模块 | JSON 文件 | 数据模型 |
|------|-----------|---------|
| 发音实验室 (9b) | `assets/data/minimal_pairs.json` | 6 音素分类 + 最小对立体 |
| 句型操练 (9c) | `assets/data/drill_*.json` | DrillCollection → DrillSet → DrillStep 三层嵌套 |
| 情景对话 (9d) | `assets/data/scenarios.json` | 场景 + 角色 + 对话线 |
| 听力跟读 (10) | `assets/data/listening.json` | 对话 + 短句列表（复用 listening JSON 数据） |

## Room Entity 映射总结

| Room Entity | SQLite Table | 所在包 |
|-------------|-------------|--------|
| WordEntity | words | word/ |
| LinkingEntity | linking | linking/ |
| ListeningAudioEntity | listening_audios | listening/ |
| ListeningQuestionEntity | listening_questions | listening/ |
| UserProgressEntity | user_progress | progress/ |
| PracticeRecordEntity | practice_records | progress/ |

## DAO 命名规则
- 查询: `get*` / `getAll*`
- 插入: `insert*`
- 更新: `update*`
- 删除: `delete*`

## 数据库迁移历史

| 版本 | 变更内容 | 对应 Phase |
|------|---------|-----------|
| v1 | 初始（5 表：words / linking / listening_audios / listening_questions / user_progress） | Phase 2 |
| v1→v2 | 新增 practice_records 表 + 索引 | Phase 8 |
| v2→v3 | linking 表新增 ipa TEXT 列 | Phase 4（扩展） |

## 预置 .db 文件的 Schema 校验
- Room 在首次安装时会用预置 .db 文件的表结构与 Entity 注解逐项比对
- 常见校验失败原因：
  - Entity 字段名与列名不一致（检查 `@ColumnInfo(name = ...)`）
  - NOT NULL 约束不匹配
  - 外键 / 索引缺失
  - 默认值（DEFAULT）不匹配
- 详细校验清单见知识库：`room-createfromasset-schema校验清单.md`
