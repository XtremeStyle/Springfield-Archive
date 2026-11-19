-- V6. Retratos para expedientes editoriales creados antes de la sincronización.
-- Solo se copia desde un personaje importado con nombre EXACTO y ÚNICO.
-- No modifica biografías, slugs, identidades ni los registros originales V2.
WITH names_with_one_external AS (
    SELECT lower(btrim(name)) AS normalized_name
      FROM characters
     WHERE external_id IS NOT NULL
  GROUP BY lower(btrim(name))
    HAVING count(*) = 1
)
UPDATE characters AS editorial
   SET portrait_path = imported.portrait_path
  FROM characters AS imported
  JOIN names_with_one_external AS unique_name
    ON lower(btrim(imported.name)) = unique_name.normalized_name
 WHERE editorial.external_id IS NULL
   AND imported.external_id IS NOT NULL
   AND imported.portrait_path IS NOT NULL
   AND btrim(imported.portrait_path) <> ''
   AND (editorial.portrait_path IS NULL OR btrim(editorial.portrait_path) = '')
   AND lower(btrim(editorial.name)) = unique_name.normalized_name;
