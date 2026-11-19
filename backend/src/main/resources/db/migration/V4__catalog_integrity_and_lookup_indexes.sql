-- V4: reglas de integridad para IDs importados + índices usados por filtros y estadísticas.
-- Migración incremental: conserva intactas las versiones aplicadas V1/V2/V3.
ALTER TABLE characters ADD CONSTRAINT ck_character_external_id_positive
    CHECK (external_id IS NULL OR external_id > 0);
ALTER TABLE episodes ADD CONSTRAINT ck_episode_external_id_positive
    CHECK (external_id IS NULL OR external_id > 0);
ALTER TABLE locations ADD CONSTRAINT ck_location_external_id_positive
    CHECK (external_id IS NULL OR external_id > 0);

CREATE INDEX IF NOT EXISTS idx_characters_status ON characters(status);
CREATE INDEX IF NOT EXISTS idx_locations_type ON locations(type);
CREATE INDEX IF NOT EXISTS idx_sync_state_updated_at ON sync_state(updated_at DESC);
