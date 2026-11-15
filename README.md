# Sentence Builder – Team 22

CS4485 Senior Design: a JavaFX application that imports public-domain text into **MySQL**, stores word / follower statistics, generates sentences with multiple algorithms, and offers next-word autocomplete.

## Team

| Member | Role |
|--------|------|
| Ved Mahant | Project Manager / User Interface Lead |
| Nirav Rohra | Technical Lead / Database / Integration |
| Umair Malik | Generation Algorithms / Configuration Manager |
| Ahmed Sherwani | QA / Text Import / Reporting |

## Features (work packages)

- **WP3** – MySQL schema + ER notes (`db/schema.sql`, `db/ER.md`)
- **WP4** – JDBC data access (`JdbcDataService`, `DatabaseConnection`)
- **WP5** – Text import with progress + tokenizer
- **WP6** – Probability-weighted, most-frequent, and random-walk generation (+ batch feedback into corpus)
- **WP7** – JavaFX screens (Import, Generate, Autocomplete, Word Editor, Reports)
- **WP8–WP9** – Autocomplete follow recording; word editor; reports with duplicate flags

If MySQL is down, the app **falls back** to an in-memory stub so demos still work.

## Requirements

- JDK 21
- Apache Maven 3.9+
- AWS RDS MySQL 8 or local MySQL 8

## Run the application

Without AWS RDS MySQL the app uses the in-memory stub automatically. With MySQL running and credentials matching `application.properties`, it uses JDBC. Cd into the project root and run:

```bash
mvn -version
DB_PREFER_JDBC=false mvn clean javafx:run
```

### Connect to AWS RDS MySQL database

Default JDBC settings to connect to database is in `application.properties`: host `localhost`, port `3306`, database `sentence_builder`, user `sentence`, password `sentence`. To get credentials for database reach out to the team or Ahmed Sherwani.

### Keyboard shortcuts

| Shortcut | Screen |
|----------|--------|
| Ctrl/Cmd+1 | Import |
| Ctrl/Cmd+2 | Generate |
| Ctrl/Cmd+3 | Autocomplete |
| Ctrl/Cmd+4 | Word Editor |
| Ctrl/Cmd+5 | Reports |

### Try import

To import a book visit https://www.gutenberg.org/ and select a book. Or use `data/sample-corpus.txt`. To import, navigate to the Import screen (Browse → Start Import). 

## Project layout

```
sentence-builder/
  db/schema.sql          MySQL DDL
  db/ER.md               Entity-relationship overview
  docker-compose.yml     Local MySQL 8
  data/sample-corpus.txt Test text
  src/main/java/.../
    SentenceBuilderApp.java
    db/                  DatabaseConfig, DatabaseConnection
    importing/           TextTokenizer
    generation/          FollowerPicker
    model/               Domain objects
    service/             DataService, JdbcDataService, StubDataService
    ui/                  FXML controllers
  src/main/resources/
    application.properties
    fxml/  css/
```

## Configuration

Override via environment variables or a local `.env` (see `.env.example`):

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`
- `DB_PREFER_JDBC=false` forces the in-memory stub
