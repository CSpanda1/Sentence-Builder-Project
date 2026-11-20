# Sentence Builder – Team 22

CS4485 Senior Design Project for Professor John Cole: A JavaFX application that imports text into **MySQL**, stores words, statistics, generates sentences with multiple algorithms, and offers next word autocompletion.

## Team

| Member | Responsibility  |
|--------|------|
| Ved Mahant | JavaFX, database implementation, and book importer UI   |
| Nirav Rohra | JavaFX, database implementation,and book importer UI   |
| Umair Malik |Algorithm generation, database implementation, and book importer UI    |
| Ahmed Sherwani | Database implementation, book importer, and algorithm generation|

## Features (work packages)

-  MySQL schema + ER notes (`db/schema.sql`, `db/ER.md`)
– JDBC data access (`JdbcDataService`, `DatabaseConnection`)
– Text import with progress + tokenizer
– Probability-weighted, most-frequent, and random-walk generation (+ batch feedback into corpus)
– JavaFX screens (Import, Generate, Autocomplete, Word Editor, Reports)
– Autocomplete follow recording; word editor; reports with duplicate flags

If MySQL is down, the app **falls back** to an in memory stub so demos still work.

## Requirements

- JDK 21
- Apache Maven 3.9+
- AWS RDS MySQL 8 or local MySQL 8

## Run the application

Without AWS RDS MySQL the app uses the in memory stub automatically. With MySQL running and credentials matching `application.properties`, it uses JDBC. Cd into the project root and run:

```bash
mvn -version
mvn clean javafx:run
```

### Connect to AWS RDS MySQL database
To get credentials for database reach out to the team or Ahmed Sherwani.
Default JDBC settings to connect to database is in `application.properties`: host `localhost`, port `3306`, database `sentence_builder`, user `sentence`, password `sentence`. To run with database do:


```bash
mvn -version
DB_PREFER_JDBC=true mvn clean javafx:run
```


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
