"""
Generate spokeneasy.db from JSON data files.
Reads app/src/main/assets/data/*.json, creates/populates the SQLite database.
"""

import json
import sqlite3
import os
from pathlib import Path

# Paths
PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "data"
DB_DIR = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "database"
DB_PATH = DB_DIR / "spokeneasy.db"


def create_schema(conn):
    conn.executescript("""
        CREATE TABLE IF NOT EXISTS words (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            word TEXT,
            phonetic TEXT,
            sentence1_en TEXT,
            sentence1_cn TEXT,
            sentence2_en TEXT,
            sentence2_cn TEXT,
            sentence3_en TEXT,
            sentence3_cn TEXT,
            category TEXT
        );

        CREATE TABLE IF NOT EXISTS linking (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            rule_name TEXT,
            original TEXT,
            linking_text TEXT,
            ipa TEXT,
            example_en TEXT,
            example_cn TEXT,
            category TEXT
        );

        CREATE TABLE IF NOT EXISTS listening_audios (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            title TEXT,
            level INTEGER NOT NULL,
            dialog_text TEXT,
            audio_file_name TEXT
        );

        CREATE TABLE IF NOT EXISTS listening_questions (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            audio_id INTEGER NOT NULL,
            question TEXT,
            option_a TEXT,
            option_b TEXT,
            option_c TEXT,
            correct_answer TEXT,
            FOREIGN KEY (audio_id) REFERENCES listening_audios(id) ON DELETE CASCADE
        );

        CREATE TABLE IF NOT EXISTS user_progress (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            user_uuid TEXT,
            module_type TEXT,
            item_id INTEGER NOT NULL,
            score INTEGER,
            is_completed INTEGER NOT NULL,
            completed_at INTEGER,
            UNIQUE(user_uuid, module_type, item_id)
        );
    """)


def load_json(filename):
    path = DATA_DIR / filename
    if not path.exists():
        print(f"  [SKIP] {filename} not found")
        return None
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def import_words(conn, data):
    if not data:
        return 0
    words = data["words"]
    cursor = conn.cursor()
    for w in words:
        cursor.execute(
            """INSERT INTO words (word, phonetic, sentence1_en, sentence1_cn,
               sentence2_en, sentence2_cn, sentence3_en, sentence3_cn, category)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (
                w["word"], w.get("phonetic", ""),
                w["sentence1_en"], w["sentence1_cn"],
                w["sentence2_en"], w["sentence2_cn"],
                w["sentence3_en"], w["sentence3_cn"],
                w.get("category", ""),
            ),
        )
    conn.commit()
    return len(words)


def import_linking(conn, data):
    if not data:
        return 0
    items = data["linking"]
    cursor = conn.cursor()
    for item in items:
        cursor.execute(
            """INSERT INTO linking (rule_name, original, linking_text,
               ipa, example_en, example_cn, category)
               VALUES (?, ?, ?, ?, ?, ?, ?)""",
            (
                item["rule_name"], item["original"], item["linking_text"],
                item.get("ipa", ""),
                item["example_en"], item["example_cn"],
                item.get("category", ""),
            ),
        )
    conn.commit()
    return len(items)


def import_listening(conn, data):
    """Import listening audios and their questions."""
    if not data:
        return 0, 0
    items = data["audios"]
    cursor = conn.cursor()
    audio_count = 0
    question_count = 0
    for item in items:
        cursor.execute(
            """INSERT INTO listening_audios (title, level, dialog_text, audio_file_name)
               VALUES (?, ?, ?, ?)""",
            (item["title"], item["level"], item["dialog_text"],
             item.get("audio_file_name", "")),
        )
        audio_id = cursor.lastrowid
        audio_count += 1

        for q in item.get("questions", []):
            cursor.execute(
                """INSERT INTO listening_questions
                   (audio_id, question, option_a, option_b, option_c, correct_answer)
                   VALUES (?, ?, ?, ?, ?, ?)""",
                (audio_id, q["question"], q["option_a"], q["option_b"],
                 q["option_c"], q["correct_answer"]),
            )
            question_count += 1

    conn.commit()
    return audio_count, question_count


def main():
    print("=== SpokenEasy Database Generator ===\n")

    # Ensure DB directory exists
    DB_DIR.mkdir(parents=True, exist_ok=True)

    # Remove existing db to start fresh
    if DB_PATH.exists():
        DB_PATH.unlink()
        print(f"  Removed existing: {DB_PATH}")

    # Create database
    conn = sqlite3.connect(str(DB_PATH))
    create_schema(conn)
    # Set user_version for Room's createFromAsset migration strategy
    conn.execute("PRAGMA user_version = 3")
    conn.commit()
    print("  Schema created (5 tables), user_version=3\n")

    # Import words
    words_data = load_json("words.json")
    word_count = import_words(conn, words_data)
    print(f"  Words: {word_count} inserted")

    # Import linking
    linking_data = load_json("linking.json")
    linking_count = import_linking(conn, linking_data)
    print(f"  Linking: {linking_count} inserted")

    # Import listening
    listening_data = load_json("listening.json")
    audio_count, question_count = import_listening(conn, listening_data)
    print(f"  Listening Audios: {audio_count} inserted")
    print(f"  Listening Questions: {question_count} inserted")

    # Verify
    cursor = conn.cursor()
    cursor.execute("SELECT COUNT(*) FROM words")
    print(f"\n  Verification - words: {cursor.fetchone()[0]}")
    cursor.execute("SELECT COUNT(*) FROM linking")
    print(f"  Verification - linking: {cursor.fetchone()[0]}")
    cursor.execute("SELECT COUNT(*) FROM listening_audios")
    print(f"  Verification - listening_audios: {cursor.fetchone()[0]}")
    cursor.execute("SELECT COUNT(*) FROM listening_questions")
    print(f"  Verification - listening_questions: {cursor.fetchone()[0]}")

    conn.close()
    db_size = os.path.getsize(DB_PATH)
    print(f"\n  Database file: {DB_PATH}")
    print(f"  Size: {db_size:,} bytes")
    print("\n=== Done ===")


if __name__ == "__main__":
    main()
