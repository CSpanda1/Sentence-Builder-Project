/******************************************************************************
 * schema.sql – Team 22 Sentence Builder (canonical MySQL schema)
 *
 * Based on Umair Malik's WP3 design, with Ahmed Sherwani import fields
 * (book_title) retained for Gutenberg book imports.
 *
 * Tables:
 *   word, follows, imported_file, file_word,
 *   sentence, generated_sentence, typed_sentence, sentence_word
 *
 * Authors: Umair Malik, Nirav Rohra, Ahmed Sherwani
 * Date: 2026-11-13
 ******************************************************************************/

CREATE DATABASE IF NOT EXISTS sentence_builder
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE sentence_builder;

CREATE TABLE IF NOT EXISTS word (
    word_id      INT          NOT NULL AUTO_INCREMENT,
    word_text    VARCHAR(100) NOT NULL,
    total_count  INT          NOT NULL DEFAULT 0,
    start_count  INT          NOT NULL DEFAULT 0,
    end_count    INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (word_id),
    UNIQUE KEY uk_word_text (word_text)
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS imported_file (
    file_id     INT           NOT NULL AUTO_INCREMENT,
    file_name   VARCHAR(255)  NOT NULL,
    file_path   VARCHAR(1024) NULL,
    book_title  VARCHAR(255)  NULL,
    word_count  INT           NOT NULL DEFAULT 0,
    unique_word_count INT     NOT NULL DEFAULT 0,
    import_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (file_id)
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sentence (
    sentence_id   INT                         NOT NULL AUTO_INCREMENT,
    sentence_text TEXT                        NOT NULL,
    start_word_id INT                         NULL,
    sentence_type ENUM('GENERATED', 'TYPED', 'BOOK')  NOT NULL,
    created_at    DATETIME                    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sentence_id),
    CONSTRAINT fk_sentence_start_word FOREIGN KEY (start_word_id)
        REFERENCES word (word_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS generated_sentence (
    sentence_id     INT         NOT NULL,
    algorithm       VARCHAR(50) NOT NULL,
    is_duplicate    BOOLEAN     NOT NULL DEFAULT FALSE,
    added_to_corpus BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (sentence_id),
    CONSTRAINT fk_generated_sentence FOREIGN KEY (sentence_id)
        REFERENCES sentence (sentence_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS typed_sentence (
    sentence_id          INT NOT NULL,
    accepted_suggestions INT NOT NULL DEFAULT 0,
    PRIMARY KEY (sentence_id),
    CONSTRAINT fk_typed_sentence FOREIGN KEY (sentence_id)
        REFERENCES sentence (sentence_id)
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;
