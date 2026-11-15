-- =============================================================================
-- Sentence Builder – Team 22
-- MySQL 8 DDL (single source of truth for the schema)
-- Author: Nirav Rohra
-- Date: 2026-10-16
--
-- Tables: words, word_pairs, imported_files, generated_sentences, autocomplete_events
-- =============================================================================

CREATE DATABASE IF NOT EXISTS sentence_builder
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE sentence_builder;

-- ---------------------------------------------------------------------------
-- words: each distinct token observed in the corpus
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS words (
    word_id           BIGINT       NOT NULL AUTO_INCREMENT,
    word_text         VARCHAR(128) NOT NULL,
    frequency         INT          NOT NULL DEFAULT 1,
    is_sentence_start TINYINT(1)   NOT NULL DEFAULT 0,
    is_sentence_end   TINYINT(1)   NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (word_id),
    UNIQUE KEY uq_words_text (word_text),
    KEY idx_words_frequency (frequency)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- word_pairs: word-follows-word relationships with observed counts
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS word_pairs (
    pair_id         BIGINT       NOT NULL AUTO_INCREMENT,
    leading_word_id BIGINT       NOT NULL,
    following_word_id BIGINT     NOT NULL,
    follow_count    INT          NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (pair_id),
    UNIQUE KEY uq_word_pairs (leading_word_id, following_word_id),
    KEY idx_pairs_leading (leading_word_id),
    KEY idx_pairs_following (following_word_id),
    CONSTRAINT fk_pairs_leading
        FOREIGN KEY (leading_word_id) REFERENCES words (word_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pairs_following
        FOREIGN KEY (following_word_id) REFERENCES words (word_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- imported_files: history of text files loaded into the corpus
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS imported_files (
    file_id           BIGINT       NOT NULL AUTO_INCREMENT,
    file_name         VARCHAR(255) NOT NULL,
    file_path         VARCHAR(1024) NOT NULL,
    word_count        INT          NOT NULL DEFAULT 0,
    unique_word_count INT          NOT NULL DEFAULT 0,
    imported_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (file_id),
    KEY idx_imported_at (imported_at)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- generated_sentences: sentences produced by generation algorithms
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS generated_sentences (
    sentence_id   BIGINT       NOT NULL AUTO_INCREMENT,
    sentence_text VARCHAR(2000) NOT NULL,
    algorithm     VARCHAR(64)  NOT NULL,
    start_word    VARCHAR(128) NOT NULL,
    is_duplicate  TINYINT(1)   NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sentence_id),
    KEY idx_sentences_created (created_at),
    KEY idx_sentences_text (sentence_text(191))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- autocomplete_events: metadata for next-word suggestion usage (WP8)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS autocomplete_events (
    event_id          BIGINT       NOT NULL AUTO_INCREMENT,
    leading_word_id   BIGINT       NULL,
    selected_word_id  BIGINT       NULL,
    leading_text      VARCHAR(128) NOT NULL,
    selected_text     VARCHAR(128) NOT NULL,
    was_unknown       TINYINT(1)   NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id),
    KEY idx_ac_created (created_at),
    CONSTRAINT fk_ac_leading
        FOREIGN KEY (leading_word_id) REFERENCES words (word_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_ac_selected
        FOREIGN KEY (selected_word_id) REFERENCES words (word_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;
