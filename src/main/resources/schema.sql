/******************************************************************************
 * schema.sql
 *
 * Comments in this file were generated in part by "Claude Fable 5.1".
 *
 * Creates the sentence_builder database and every table the Sentence Builder
 * application uses. Run this once against a MySQL 8 server to set up an empty
 * database. Every statement uses IF NOT EXISTS, so running it again on a
 * server that already has the tables is harmless and leaves existing data
 * alone.
 *
 * How to run from MySQL Workbench: open this file with File > Open SQL Script,
 * then press the lightning bolt (Execute All).
 * How to run from the command line: mysql -h <host> -u <user> -p < schema.sql
 *
 * Table overview:
 *   word                one row per unique word, with its counts
 *   follows             one row per (first word, next word) pair, with how
 *                       many times the pair was seen
 *   imported_file       one row per text file loaded into the database
 *   file_word           how many times each word appeared in each file
 *   sentence            one row per sentence the app generated or the user
 *                       typed on the auto-complete screen
 *   generated_sentence  extra columns for sentences the app generated
 *   typed_sentence      extra columns for sentences the user typed
 *   sentence_word       which words each sentence uses and how often
 *
 * Written by Umair Malik (UAM200000) for CS4485.0W1, Sentence Builder project,
 * starting October 13, 2026.
 ******************************************************************************/

CREATE DATABASE IF NOT EXISTS sentence_builder;

USE sentence_builder;

/*
 * word
 *
 * The vocabulary. total_count is how many times the word has been seen in
 * all imported text. start_count and end_count track how often the word
 * began or ended a sentence, which the generators use to pick a first word
 * and to decide when a sentence can stop.
 *
 * word_text is UNIQUE so the importer can look a word up by its text and
 * either insert it or bump its counts. The default collation is case
 * insensitive, so "The" and "the" are stored as one word.
 *
 * Umair Malik, October 13, 2026.
 */
CREATE TABLE IF NOT EXISTS word (
    word_id      INT          NOT NULL AUTO_INCREMENT,
    word_text    VARCHAR(100) NOT NULL,
    total_count  INT          NOT NULL DEFAULT 0,
    start_count  INT          NOT NULL DEFAULT 0,
    end_count    INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (word_id),
    UNIQUE KEY uk_word_text (word_text)
);

/*
 * follows
 *
 * The heart of the generator. One row says "next_word_id came right after
 * first_word_id, follow_count times". Both columns point at the word table.
 * The UNIQUE key on the pair means a pair is stored once and its count is
 * incremented, and it also makes "give me every follower of word X" a fast
 * lookup because first_word_id is the leading column of that index.
 *
 * Umair Malik, October 13, 2026.
 */
CREATE TABLE IF NOT EXISTS follows (
    follow_id     INT NOT NULL AUTO_INCREMENT,
    first_word_id INT NOT NULL,
    next_word_id  INT NOT NULL,
    follow_count  INT NOT NULL DEFAULT 1,
    PRIMARY KEY (follow_id),
    UNIQUE KEY uk_follows_pair (first_word_id, next_word_id),
    CONSTRAINT fk_follows_first_word FOREIGN KEY (first_word_id)
        REFERENCES word (word_id),
    CONSTRAINT fk_follows_next_word FOREIGN KEY (next_word_id)
        REFERENCES word (word_id)
);

/*
 * imported_file
 *
 * One row per text file the importer has loaded. word_count is the number of
 * words read from that file and import_time is when the import finished.
 * file_path is kept so the file can be shown in reports and so the team can
 * tell whether a file was already imported.
 *
 * Umair Malik, October 13, 2026.
 */
CREATE TABLE IF NOT EXISTS imported_file (
    file_id     INT           NOT NULL AUTO_INCREMENT,
    file_name   VARCHAR(255)  NOT NULL,
    file_path   VARCHAR(1024) NULL,
    word_count  INT           NOT NULL DEFAULT 0,
    import_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (file_id)
);

/*
 * file_word
 *
 * Links words to the files they came from, with a per file count. This is
 * not required by the project spec but it makes per file reports possible
 * and costs little, since the importer already knows the counts.
 *
 * Umair Malik, October 13, 2026.
 */
CREATE TABLE IF NOT EXISTS file_word (
    file_word_id  INT NOT NULL AUTO_INCREMENT,
    file_id       INT NOT NULL,
    word_id       INT NOT NULL,
    count_in_file INT NOT NULL DEFAULT 1,
    PRIMARY KEY (file_word_id),
    UNIQUE KEY uk_file_word (file_id, word_id),
    CONSTRAINT fk_file_word_file FOREIGN KEY (file_id)
        REFERENCES imported_file (file_id),
    CONSTRAINT fk_file_word_word FOREIGN KEY (word_id)
        REFERENCES word (word_id)
);

/*
 * sentence
 *
 * Every sentence the app produced or the user typed. The history report
 * reads this table to show past sentences and to spot duplicates.
 * sentence_type says which of the two detail tables below has the extra
 * columns for this row. start_word_id is the word the sentence was built
 * from and may be NULL for typed sentences.
 *
 * Umair Malik, October 13, 2026.
 */
CREATE TABLE IF NOT EXISTS sentence (
    sentence_id   INT                         NOT NULL AUTO_INCREMENT,
    sentence_text TEXT                        NOT NULL,
    start_word_id INT                         NULL,
    sentence_type ENUM('GENERATED', 'TYPED')  NOT NULL,
    created_at    DATETIME                    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sentence_id),
    CONSTRAINT fk_sentence_start_word FOREIGN KEY (start_word_id)
        REFERENCES word (word_id)
);

/*
 * generated_sentence
 *
 * Extra columns for sentences the app generated. It shares its primary key
 * with sentence, so there is at most one of these rows per sentence.
 * algorithm names the generator class that produced the sentence.
 * added_to_corpus records whether the batch was fed back into the word data
 * with the "add to corpus" button, so the same batch is not added twice.
 *
 * Umair Malik, October 13, 2026.
 */
CREATE TABLE IF NOT EXISTS generated_sentence (
    sentence_id     INT         NOT NULL,
    algorithm       VARCHAR(50) NOT NULL,
    added_to_corpus BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (sentence_id),
    CONSTRAINT fk_generated_sentence FOREIGN KEY (sentence_id)
        REFERENCES sentence (sentence_id)
);

/*
 * typed_sentence
 *
 * Extra columns for sentences the user typed on the auto-complete screen.
 * accepted_suggestions counts how many suggested words the user picked,
 * which is a simple measure of how useful the suggestions were.
 *
 * Umair Malik, October 13, 2026.
 */
CREATE TABLE IF NOT EXISTS typed_sentence (
    sentence_id          INT NOT NULL,
    accepted_suggestions INT NOT NULL DEFAULT 0,
    PRIMARY KEY (sentence_id),
    CONSTRAINT fk_typed_sentence FOREIGN KEY (sentence_id)
        REFERENCES sentence (sentence_id)
);

/*
 * sentence_word
 *
 * Which words appear in each sentence and how many times. Lets reports
 * answer questions like "which sentences used this word" without parsing
 * sentence_text again.
 *
 * Umair Malik, October 13, 2026.
 */
CREATE TABLE IF NOT EXISTS sentence_word (
    sentence_word_id INT NOT NULL AUTO_INCREMENT,
    sentence_id      INT NOT NULL,
    word_id          INT NOT NULL,
    occurrences      INT NOT NULL DEFAULT 1,
    PRIMARY KEY (sentence_word_id),
    UNIQUE KEY uk_sentence_word (sentence_id, word_id),
    CONSTRAINT fk_sentence_word_sentence FOREIGN KEY (sentence_id)
        REFERENCES sentence (sentence_id),
    CONSTRAINT fk_sentence_word_word FOREIGN KEY (word_id)
        REFERENCES word (word_id)
);
