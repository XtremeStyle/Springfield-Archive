package com.springfieldarchive.catalog;

/**
 * Construye exclusivamente vistas SQL con tablas y columnas controladas por la aplicación.
 *
 * La base conserva los datos editoriales y los externos por separado. La vista pública consulta únicamente
 * registros importados; sin sincronizar devuelve un catálogo vacío en lugar
 * de disfrazar los registros editoriales como datos de la API.
 *
 * El modo "all" sigue disponible para auditoría explícita.
 */
final class CatalogVisibility {
    private CatalogVisibility() { }

    static String from(String source, String table, String columns, String imageColumn) {
        if (!"curated".equals(source) && !"api".equals(source)) return table;
        if (!"characters".equals(table) && !"locations".equals(table)) {
            throw new IllegalArgumentException("Tabla de catálogo desconocida");
        }
        if (!("portrait_path".equals(imageColumn) || "image_path".equals(imageColumn))) {
            throw new IllegalArgumentException("Columna de imagen desconocida");
        }
        // El catálogo público usa EXCLUSIVAMENTE registros con identidad del proveedor.
        // Las fichas V2 conservan su propia procedencia y sólo se consultan
        // en modo editorial/all. Nunca se disfrazan de contenido sincronizado.
        // Un conjunto aún sin sincronizar devuelve cero resultados reales.
        String publicRows = "external_id IS NOT NULL";
        return "(SELECT " + columns + ", ROW_NUMBER() OVER (PARTITION BY "
                + "lower(regexp_replace(btrim(name), '[^[:alnum:]]+', '', 'g')) "
                + "ORDER BY (NULLIF(btrim(" + imageColumn + "), '') IS NULL) ASC, "
                + "external_id ASC NULLS LAST, id ASC) AS archive_rank "
                + "FROM " + table + " WHERE " + publicRows + ") AS catalog";
    }
}
