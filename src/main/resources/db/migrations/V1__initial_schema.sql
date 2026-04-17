CREATE TABLE IF NOT EXISTS citizens (
    uuid            TEXT PRIMARY KEY,
    minecraft_name  TEXT NOT NULL,
    first_name      TEXT NOT NULL,
    last_name       TEXT NOT NULL,
    birth_date      TEXT NOT NULL,
    registered_at   INTEGER NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_citizens_fullname
    ON citizens (LOWER(first_name), LOWER(last_name));

CREATE TABLE IF NOT EXISTS appearances (
    uuid        TEXT PRIMARY KEY,
    skin_tone   INTEGER NOT NULL,
    eye_color   INTEGER NOT NULL,
    hair_style  INTEGER NOT NULL,
    hair_color  INTEGER NOT NULL,
    FOREIGN KEY (uuid) REFERENCES citizens(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS id_cards (
    serial         TEXT PRIMARY KEY,
    owner_uuid     TEXT NOT NULL,
    issued_at      INTEGER NOT NULL,
    reissue_count  INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (owner_uuid) REFERENCES citizens(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS id_card_counters (
    year    INTEGER PRIMARY KEY,
    counter INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY
);
