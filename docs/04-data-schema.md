# SpokenEasy - 数据库设计

## 使用方式
预置 .db 文件 → assets/database/spokeneasy.db
Room 通过 createFromAsset() 加载

## 表结构

### words — 单词表
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

### linking — 连读表
```sql
CREATE TABLE linking (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    rule_name TEXT NOT NULL,          -- 规则名称
    original TEXT NOT NULL,           -- 原始写法 (e.g. "give me")
    linking_text TEXT NOT NULL,       -- 连读写法 (e.g. "gimme")
    example_en TEXT,                  -- 例句英文
    example_cn TEXT,                  -- 例句中文
    category TEXT                     -- 分类
);
```

### listening_audios — 听力对话表
```sql
CREATE TABLE listening_audios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,              -- 对话标题
    level INTEGER NOT NULL,           -- 难度等级 (1/2/3)
    dialog_text TEXT NOT NULL,        -- 完整对话文本
    audio_file_name TEXT              -- 音频文件名（预留）
);
```

### listening_questions — 听力题目表
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

### user_progress — 用户进度表
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

## Room Entity 映射
- WordEntity → words
- LinkingEntity → linking
- ListeningAudioEntity → listening_audios
- ListeningQuestionEntity → listening_questions
- UserProgressEntity → user_progress

## DAO 命名规则
- 查询: `get*` / `getAll*`
- 插入: `insert*`
- 更新: `update*`
- 删除: `delete*`
