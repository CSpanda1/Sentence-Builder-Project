# Sentence Builder – Entity Relationship Overview

**Author:** Nirav Rohra  
**Date:** 2026-10-16  
**Database:** `sentence_builder` (MySQL 8)

```mermaid
erDiagram
    WORDS ||--o{ WORD_PAIRS : "leads"
    WORDS ||--o{ WORD_PAIRS : "follows"
    WORDS ||--o{ AUTOCOMPLETE_EVENTS : "leading"
    WORDS ||--o{ AUTOCOMPLETE_EVENTS : "selected"
    IMPORTED_FILES ||--o{ WORDS : "populates"
    GENERATED_SENTENCES }o--|| WORDS : "starts_with"

    WORDS {
        BIGINT word_id PK
        VARCHAR word_text UK
        INT frequency
        BOOL is_sentence_start
        BOOL is_sentence_end
    }

    WORD_PAIRS {
        BIGINT pair_id PK
        BIGINT leading_word_id FK
        BIGINT following_word_id FK
        INT follow_count
    }

    IMPORTED_FILES {
        BIGINT file_id PK
        VARCHAR file_name
        VARCHAR file_path
        INT word_count
        INT unique_word_count
        TIMESTAMP imported_at
    }

    GENERATED_SENTENCES {
        BIGINT sentence_id PK
        VARCHAR sentence_text
        VARCHAR algorithm
        VARCHAR start_word
        BOOL is_duplicate
        TIMESTAMP created_at
    }

    AUTOCOMPLETE_EVENTS {
        BIGINT event_id PK
        BIGINT leading_word_id FK
        BIGINT selected_word_id FK
        VARCHAR leading_text
        VARCHAR selected_text
        BOOL was_unknown
    }
```

## Relationships

| From | To | Cardinality | Meaning |
|------|-----|-------------|---------|
| `words` | `word_pairs` | 1:N (as leading) | A word may lead many follower pairs |
| `words` | `word_pairs` | 1:N (as following) | A word may follow many leaders |
| `words` | `autocomplete_events` | 1:N | Suggestion acceptances / unknown-word adds |
| `imported_files` | corpus | logical | Import history; tokens land in `words` / `word_pairs` |
| `generated_sentences` | corpus | logical | Generated text can be fed back into the corpus |

Apply the schema with:

```bash
mysql -u root -p < db/schema.sql
```

Or start the Docker MySQL service (see README) which runs this script on first boot.
