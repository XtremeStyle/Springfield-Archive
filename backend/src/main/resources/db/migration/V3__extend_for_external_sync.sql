-- V3__extend_for_external_sync.sql
-- Amplía el modelo para los tres recursos sincronizados desde The Simpsons API.
-- No modifica V1/V2 ni sus restricciones; sólo amplía columnas (drop NOT NULL en description).
-- Manual seed (V2) se conserva: las nuevas columnas quedan NULL en esos registros.

ALTER TABLE characters ALTER COLUMN description DROP NOT NULL;
ALTER TABLE locations  ALTER COLUMN description DROP NOT NULL;

-- Episodios (nuevo recurso)
CREATE TABLE episodes (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    external_id    INTEGER UNIQUE,
    slug           TEXT NOT NULL UNIQUE
                   CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$' AND char_length(slug) BETWEEN 1 AND 120),
    season         SMALLINT CHECK (season IS NULL OR (season >= 0 AND season <= 100)),
    episode_number SMALLINT CHECK (episode_number IS NULL OR (episode_number >= 0 AND episode_number <= 1000)),
    airdate        DATE,
    name           TEXT NOT NULL
                   CHECK (name !~ '^[[:space:]]*$' AND char_length(name) BETWEEN 1 AND 200),
    synopsis       TEXT,
    description    TEXT,
    image_path     TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Personajes
ALTER TABLE characters
    ADD COLUMN IF NOT EXISTS external_id INTEGER UNIQUE,
    ADD COLUMN IF NOT EXISTS birthdate DATE,
    ADD COLUMN IF NOT EXISTS gender TEXT,
    ADD COLUMN IF NOT EXISTS phrases TEXT[],
    ADD COLUMN IF NOT EXISTS portrait_path TEXT,
    ADD COLUMN IF NOT EXISTS first_appearance_ep_id INTEGER,
    ADD COLUMN IF NOT EXISTS first_appearance_sh_id INTEGER;

-- Lugares
ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS external_id INTEGER UNIQUE,
    ADD COLUMN IF NOT EXISTS town TEXT,
    ADD COLUMN IF NOT EXISTS use TEXT,
    ADD COLUMN IF NOT EXISTS image_path TEXT,
    ADD COLUMN IF NOT EXISTS first_appearance_ep_id INTEGER,
    ADD COLUMN IF NOT EXISTS first_appearance_sh_id INTEGER;

-- Índices para catálogo, búsqueda y sincronización
CREATE INDEX IF NOT EXISTS idx_episodes_external_id        ON episodes(external_id);
CREATE INDEX IF NOT EXISTS idx_episodes_slug               ON episodes(slug);
CREATE INDEX IF NOT EXISTS idx_episodes_name               ON episodes(name);
CREATE INDEX IF NOT EXISTS idx_episodes_season_number      ON episodes(season, episode_number);
CREATE INDEX IF NOT EXISTS idx_characters_external_id      ON characters(external_id);
CREATE INDEX IF NOT EXISTS idx_characters_name             ON characters(name);
CREATE INDEX IF NOT EXISTS idx_locations_external_id       ON locations(external_id);
CREATE INDEX IF NOT EXISTS idx_locations_name              ON locations(name);

-- Registro de sincronización (uno por recurso)
CREATE TABLE sync_state (
    resource    TEXT PRIMARY KEY CHECK (resource IN ('characters', 'episodes', 'locations')),
    status      TEXT NOT NULL CHECK (status IN ('idle', 'running', 'completed', 'failed')),
    last_page   INTEGER NOT NULL DEFAULT 0,
    processed   INTEGER NOT NULL DEFAULT 0,
    errors      INTEGER NOT NULL DEFAULT 0,
    synced_at   TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    error       TEXT
);

INSERT INTO sync_state (resource, status)
VALUES ('characters', 'idle'), ('episodes', 'idle'), ('locations', 'idle')
ON CONFLICT DO NOTHING;
