-- V1__create_catalog.sql
-- Catálogo Springfield Archive: CHARACTER y LOCATION (modelo lógico)
-- Tablas con nombres plurales y seguros para PostgreSQL; 'character' es
-- palabra clave del estándar y se evita por robustez.
-- Sin edad, sin relaciones especulativas, sin columnas extra.
-- Las cadenas compuestas exclusivamente de whitespace se rechazan con
-- la clase [[:space:]] de las expresiones regulares POSIX de PostgreSQL.

CREATE TABLE characters (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug        TEXT NOT NULL UNIQUE
                CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$' AND char_length(slug) BETWEEN 1 AND 80),
    name        TEXT NOT NULL
                CHECK (name !~ '^[[:space:]]*$' AND char_length(name) BETWEEN 1 AND 120),
    occupation  TEXT
                CHECK (occupation IS NULL
                       OR (occupation !~ '^[[:space:]]*$' AND char_length(occupation) <= 255)),
    status      TEXT NOT NULL
                CHECK (status IN ('Alive', 'Deceased', 'Unknown')),
    description TEXT NOT NULL
                CHECK (description !~ '^[[:space:]]*$' AND char_length(description) BETWEEN 1 AND 2000),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE locations (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug        TEXT NOT NULL UNIQUE
                CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$' AND char_length(slug) BETWEEN 1 AND 80),
    name        TEXT NOT NULL
                CHECK (name !~ '^[[:space:]]*$' AND char_length(name) BETWEEN 1 AND 120),
    type        TEXT NOT NULL
                CHECK (type IN ('home', 'business', 'landmark', 'school', 'park', 'plant')),
    description TEXT NOT NULL
                CHECK (description !~ '^[[:space:]]*$' AND char_length(description) BETWEEN 1 AND 2000),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
