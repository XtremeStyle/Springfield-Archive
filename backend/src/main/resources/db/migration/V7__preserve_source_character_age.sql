-- The Simpsons API incluye age en los personajes. Las versiones anteriores
-- lo desechaban durante la importación; se preserva ahora sin reescribir V1-V6.
-- La edad en una serie animada puede ser irreal (Burns, por ejemplo); no
-- imponemos un límite humano de 120 años a los datos del proveedor.
ALTER TABLE characters ADD COLUMN IF NOT EXISTS age INTEGER
    CHECK (age IS NULL OR (age >= 0 AND age <= 10000));
