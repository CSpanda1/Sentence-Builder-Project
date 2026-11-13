# Sentence Builder – Entity Relationship Overview

**Authors:** Umair Malik (schema), Ahmed Sherwani (import), Nirav Rohra (integration)  
**Database:** `sentence_builder` (MySQL 8)

```mermaid
erDiagram
    WORD ||--o{ FOLLOWS : "first"
    WORD ||--o{ FOLLOWS : "next"
    WORD ||--o{ FILE_WORD : "appears_in"
    IMPORTED_FILE ||--o{ FILE_WORD : "contains"
    WORD ||--o{ SENTENCE : "may_start"
    SENTENCE ||--o| GENERATED_SENTENCE : "detail"
    SENTENCE ||--o| TYPED_SENTENCE : "detail"
    SENTENCE ||--o{ SENTENCE_WORD : "uses"
    WORD ||--o{ SENTENCE_WORD : "in"

    WORD {
        INT word_id PK
        VARCHAR word_text UK
        INT total_count
        INT start_count
        INT end_count
    }
    FOLLOWS {
        INT follow_id PK
        INT first_word_id FK
        INT next_word_id FK
        INT follow_count
    }
    IMPORTED_FILE {
        INT file_id PK
        VARCHAR file_name
        VARCHAR book_title
        INT word_count
    }
    SENTENCE {
        INT sentence_id PK
        TEXT sentence_text
        ENUM sentence_type
    }
```

Apply with:

```bash
mysql -u root -p < db/schema.sql
```

Or point `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` at AWS RDS (Ahmed).
