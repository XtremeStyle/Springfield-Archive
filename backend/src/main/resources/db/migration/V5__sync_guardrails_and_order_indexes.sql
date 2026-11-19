-- V5: integridad del checkpoint e índices para la ordenación paginada.
-- Compatible con los datos de V1-V4; no modifica ni borra los registros del catálogo.
ALTER TABLE sync_state ADD CONSTRAINT ck_sync_progress_nonnegative
    CHECK (last_page >= 0 AND processed >= 0 AND errors >= 0);

-- Los listados ordenan por (name, id), no solamente por name.
CREATE INDEX IF NOT EXISTS idx_characters_name_id ON characters (name, id);
CREATE INDEX IF NOT EXISTS idx_episodes_name_id ON episodes (name, id);
CREATE INDEX IF NOT EXISTS idx_locations_name_id ON locations (name, id);
