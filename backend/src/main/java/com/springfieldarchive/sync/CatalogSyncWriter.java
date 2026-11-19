package com.springfieldarchive.sync;

import com.springfieldarchive.catalog.SlugUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Escritura idempotente hacia PostgreSQL para la sincronización del catálogo.
 * Upsert por external_id; colisiones de slug resueltas determinísticamente
 * sufijando con el external_id (nunca se fusiona por suposición).
 */
@Repository
public class CatalogSyncWriter {

    private final NamedParameterJdbcTemplate jdbc;

    public CatalogSyncWriter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int upsertCharacter(int externalId, String name, String occupation, String status,
                               String description, Integer age, String birthdate, String gender,
                               List<String> phrases, String portraitPath,
                               Integer firstEpId, Integer firstShId) {
        String slug = resolveSlug("characters", name, externalId);
        List<String> cleanPhrases = phrases == null ? null :
                phrases.stream()
                        .map(p -> p == null ? "" : p.replace("|||", "·").replaceAll("\\r?\\n", " "))
                        .toList();
        String sql = """
                INSERT INTO characters (slug, name, occupation, status, description, external_id,
                                        age, birthdate, gender, phrases, portrait_path,
                                        first_appearance_ep_id, first_appearance_sh_id)
                VALUES (:slug, :name, :occupation, :status, :description, :externalId,
                         :age, CAST(:birthdate AS DATE), :gender,
                        string_to_array(:phrases, '|||'), :portraitPath, :firstEp, :firstSh)
                ON CONFLICT (external_id) DO UPDATE SET
                    name = EXCLUDED.name,
                    occupation = EXCLUDED.occupation,
                    status = EXCLUDED.status,
                    description = EXCLUDED.description,
                    age = EXCLUDED.age,
                    birthdate = EXCLUDED.birthdate,
                    gender = EXCLUDED.gender,
                    phrases = EXCLUDED.phrases,
                    portrait_path = COALESCE(EXCLUDED.portrait_path, characters.portrait_path),
                    first_appearance_ep_id = EXCLUDED.first_appearance_ep_id,
                    first_appearance_sh_id = EXCLUDED.first_appearance_sh_id,
                    slug = EXCLUDED.slug
                """;
        int updated = jdbc.update(sql, new MapSqlParameterSource()
                .addValue("slug", slug)
                .addValue("name", name)
                .addValue("occupation", blankToNull(occupation))
                .addValue("status", normalizeStatus(status))
                .addValue("description", blankToNull(description))
                .addValue("externalId", externalId)
                .addValue("age", age)
                .addValue("birthdate", blankToNull(birthdate))
                .addValue("gender", blankToNull(gender))
                .addValue("phrases", cleanPhrases == null ? null : String.join("|||", cleanPhrases))
                .addValue("portraitPath", blankToNull(portraitPath))
                .addValue("firstEp", firstEpId)
                .addValue("firstSh", firstShId));
        // Los 40 expedientes editoriales V2 no tienen external_id ni retrato.
        // Solo heredamos una imagen si hay una correspondencia EXACTA y ÚNICA
        // con la ficha externa; nunca se sobrescribe su biografía editorial.
        if (portraitPath != null && !portraitPath.isBlank()) {
            jdbc.update("""
                UPDATE characters AS editorial
                   SET portrait_path = :portrait
                 WHERE editorial.external_id IS NULL
                   AND (editorial.portrait_path IS NULL OR btrim(editorial.portrait_path) = '')
                   AND lower(btrim(editorial.name)) = lower(btrim(:name))
                   AND (SELECT count(*) FROM characters AS imported
                         WHERE imported.external_id IS NOT NULL
                           AND lower(btrim(imported.name)) = lower(btrim(:name))) = 1
                """, new MapSqlParameterSource()
                    .addValue("portrait", portraitPath.trim())
                    .addValue("name", name));
        }
        return updated;
    }

    public int upsertEpisode(int externalId, Integer season, Integer episodeNumber,
                             String airdate, String name, String synopsis,
                             String description, String imagePath) {
        String seasonPart = season == null ? "s0" : "s" + season;
        String episodePart = episodeNumber == null ? "e0" : "e" + episodeNumber;
        String base = seasonPart + "-" + episodePart + "-" + SlugUtils.slugify(name);
        String slug = resolveSlug("episodes", base, externalId);

        String sql = """
                INSERT INTO episodes (external_id, slug, season, episode_number, airdate,
                                      name, synopsis, description, image_path)
                VALUES (:externalId, :slug, :season, :episodeNumber, CAST(:airdate AS DATE),
                        :name, :synopsis, :description, :imagePath)
                ON CONFLICT (external_id) DO UPDATE SET
                    slug = EXCLUDED.slug,
                    season = EXCLUDED.season,
                    episode_number = EXCLUDED.episode_number,
                    airdate = EXCLUDED.airdate,
                    name = EXCLUDED.name,
                    synopsis = EXCLUDED.synopsis,
                    description = EXCLUDED.description,
                    image_path = COALESCE(EXCLUDED.image_path, episodes.image_path)
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("externalId", externalId)
                .addValue("slug", slug)
                .addValue("season", season)
                .addValue("episodeNumber", episodeNumber)
                .addValue("airdate", blankToNull(airdate))
                .addValue("name", name)
                .addValue("synopsis", blankToNull(synopsis))
                .addValue("description", blankToNull(description))
                .addValue("imagePath", blankToNull(imagePath)));
    }

    public int upsertLocation(int externalId, String name, String town, String use,
                              String description, String imagePath,
                              Integer firstEpId, Integer firstShId) {
        String slug = resolveSlug("locations", name, externalId);

        // 'type' forma parte del esquema manual; requiere un valor permitido.
        // Reglas deterministas: si el proveedor no ofrece tipo compatible, usa "landmark".
        String type = normalizeLocationType(use);

        String sql = """
                INSERT INTO locations (slug, name, type, description, external_id,
                                       town, use, image_path,
                                       first_appearance_ep_id, first_appearance_sh_id)
                VALUES (:slug, :name, :type, :description, :externalId,
                        :town, :use, :imagePath, :firstEp, :firstSh)
                ON CONFLICT (external_id) DO UPDATE SET
                    name = EXCLUDED.name,
                    type = EXCLUDED.type,
                    description = EXCLUDED.description,
                    town = EXCLUDED.town,
                    use = EXCLUDED.use,
                    image_path = COALESCE(EXCLUDED.image_path, locations.image_path),
                    first_appearance_ep_id = EXCLUDED.first_appearance_ep_id,
                    first_appearance_sh_id = EXCLUDED.first_appearance_sh_id,
                    slug = EXCLUDED.slug
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("slug", slug)
                .addValue("name", name)
                .addValue("type", type)
                .addValue("description", blankToNull(description))
                .addValue("externalId", externalId)
                .addValue("town", blankToNull(town))
                .addValue("use", blankToNull(use))
                .addValue("imagePath", blankToNull(imagePath))
                .addValue("firstEp", firstEpId)
                .addValue("firstSh", firstShId));
    }

    /**
     * Slugs estables y dentro de la restricción SQL, incluso con títulos extensos.
     * V1 limita personajes/lugares a 80 caracteres; V3 permite 120 para episodios.
     * No fusionar nombres repetidos: cada external_id conserva su identidad.
     */
    private String resolveSlug(String table, String rawName, int externalId) {
        int maxLength = "episodes".equals(table) ? 120 : 80;
        String base = SlugUtils.slugify(rawName);
        for (int attempt = 0; attempt < 30; attempt++) {
            String suffix = attempt == 0 ? "" : "-" + externalId + (attempt == 1 ? "" : "-" + (attempt - 1));
            String candidate = trimSlug(base, maxLength - suffix.length()) + suffix;
            if (slugBelongsTo(table, candidate, externalId) || slugFree(table, candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No se pudo reservar un slug único para un recurso externo");
    }

    private static String trimSlug(String base, int maxLength) {
        if (maxLength < 1) { throw new IllegalArgumentException("Identificador externo demasiado extenso"); }
        String candidate = base.substring(0, Math.min(maxLength, base.length())).replaceAll("-+$", "");
        return candidate.isEmpty() ? "unknown" : candidate;
    }

    private boolean slugFree(String table, String slug) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE slug = :s",
                new MapSqlParameterSource("s", slug), Integer.class);
        return count == null || count == 0;
    }

    private boolean slugBelongsTo(String table, String slug, int externalId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE slug = :s AND external_id = :id",
                new MapSqlParameterSource("s", slug).addValue("id", externalId), Integer.class);
        return count != null && count > 0;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static String normalizeStatus(String status) {
        if (status == null) {
            return "Unknown";
        }
        return switch (status.trim().toLowerCase()) {
            case "alive" -> "Alive";
            case "deceased" -> "Deceased";
            default -> "Unknown";
        };
    }

    private static String normalizeLocationType(String use) {
        if (use == null) {
            return "landmark";
        }
        String u = use.trim().toLowerCase();
        return switch (u) {
            case "residential", "home", "house" -> "home";
            case "business", "commercial", "shop", "restaurant", "store", "bar" -> "business";
            case "school", "elementary", "primary", "high school" -> "school";
            case "park", "recreation", "playground" -> "park";
            case "plant", "factory", "power", "nuclear", "industrial" -> "plant";
            case "landmark", "monument", "government", "public", "municipal" -> "landmark";
            default -> "landmark";
        };
    }
}
