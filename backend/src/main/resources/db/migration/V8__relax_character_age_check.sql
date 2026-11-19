-- V8: edades ficcionales extremas publicadas por The Simpsons API.
-- V7 acotó age a 0..10000 por cautela; el proveedor publica edades como
-- 1 000 000 (p. ej. Kodos, id externo 60) y la importación completa se
-- interrumpe por ello. El dato es editorial del proveedor, no un error de
-- validación; se amplía el tope conservando INTEGER y edad no negativa.
ALTER TABLE characters DROP CONSTRAINT characters_age_check;
ALTER TABLE characters ADD CONSTRAINT characters_age_check
    CHECK (age IS NULL OR (age >= 0 AND age <= 2000000000));
