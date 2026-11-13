# Sentence Builder – Team 22

CS4485 Senior Design: JavaFX + MySQL sentence builder with Gutenberg import, multi-algorithm generation, and autocomplete.

## Team contributions integrated

| Member | What landed in this tree |
|--------|---------------------------|
| **Ahmed Sherwani** (`origin/Ahmed`) | Book import pipeline (tokenize, sentence extract, frequency / transitions, batch JDBC inserts), AWS `DATABASE_*` env support, `data/pg11.txt` |
| **Umair Malik** (`origin/Umair`) | Canonical schema (`word` / `follows` / `sentence`…), `SentenceGenerator` hierarchy, autocomplete suggester / recorder |
| **Ved Mahant / Nirav Rohra** | JavaFX screens, wiring, JDBC `DataService`, stub fallback |

## Features

- Import text / Gutenberg books with progress (Ahmed logic → Umair tables)
- Generate with probability-weighted, most-frequent, and random-walk algorithms
- Autocomplete after space / `.` / `,`
- Word editor + reports (imports, generated history, duplicates, stats)
- Runs on **stub** without MySQL; uses **JDBC** when DB is reachable

## Run

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
cd sentence-builder
mvn clean javafx:run
```

Window title shows `(Stub)` or `(MySQL)`.

### Database (when ready)

```bash
mysql -u root -p < db/schema.sql
```

Config: `src/main/resources/application.properties` or `.env` (see `.env.example`).

Ahmed’s AWS RDS style:

```bash
export DATABASE_URL='jdbc:mysql://host:3306/sentence_builder?...'
export DATABASE_USERNAME='...'
export DATABASE_PASSWORD='...'
```

### Sample import files

- `data/sample-corpus.txt` – tiny demo
- `data/pg11.txt` – Alice in Wonderland (from Ahmed’s branch)

## Layout

```
db/schema.sql, db/ER.md
data/
src/main/java/.../sentencebuilder/
  importing/BookCorpusImporter.java   # Ahmed
  generation/*                        # Umair
  autocomplete/*                      # Umair
  db/*, service/JdbcDataService.java  # Nirav integration
  ui/*                                # JavaFX WP7
```
