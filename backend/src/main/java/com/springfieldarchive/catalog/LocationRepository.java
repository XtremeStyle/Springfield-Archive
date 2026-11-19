package com.springfieldarchive.catalog;

import com.springfieldarchive.dto.LocationDto;
import com.springfieldarchive.dto.PageResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class LocationRepository {
    private static final String COLUMNS = "id, slug, name, type, description, external_id, town, use, image_path, first_appearance_ep_id, first_appearance_sh_id, created_at";
    private static final org.springframework.jdbc.core.RowMapper<LocationDto> MAPPER = (rs, rowNum) -> new LocationDto(
            rs.getLong("id"), (Integer) rs.getObject("external_id"), rs.getString("slug"), rs.getString("name"),
            rs.getString("type"), rs.getString("description"), rs.getString("town"), rs.getString("use"),
            rs.getString("image_path"), (Integer) rs.getObject("first_appearance_ep_id"),
            (Integer) rs.getObject("first_appearance_sh_id"), rs.getObject("created_at", OffsetDateTime.class));
    private final NamedParameterJdbcTemplate jdbc;
    public LocationRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public PageResponse<LocationDto> findPage(String rawQ, int page, int size) {
        return findPage(rawQ, "curated", "all", "az", "all", page, size);
    }

    public PageResponse<LocationDto> findPage(String rawQ, String source, String scope, String order,
                                               String type, int page, int size) {
        CatalogQueryValidator.requireChoice("source", source, "curated", "api", "editorial", "all");
        CatalogQueryValidator.requireChoice("scope", scope, "all", "name");
        CatalogQueryValidator.requireChoice("order", order, "relevance", "az", "za", "recent");
        CatalogQueryValidator.requireChoice("type", type, "all", "home", "business", "landmark", "school", "park", "plant");
        String q = rawQ == null ? "" : rawQ.strip();
        String from = CatalogVisibility.from(source, "locations", COLUMNS, "image_path");
        List<String> conditions = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", size).addValue("offset", page * size);
        if ("curated".equals(source) || "api".equals(source)) conditions.add("archive_rank = 1");
        if ("editorial".equals(source)) conditions.add("external_id IS NULL");
        if (!"all".equals(type)) { conditions.add("type = :type"); params.addValue("type", type); }
        if (!q.isEmpty()) {
            params.addValue("pattern", "%" + LikeEscape.escape(q) + "%");
            params.addValue("exact", q);
            params.addValue("prefix", LikeEscape.escape(q) + "%");
            String match = "name ILIKE :pattern ESCAPE '\\'";
            if ("all".equals(scope)) match += " OR type ILIKE :pattern ESCAPE '\\' OR description ILIKE :pattern ESCAPE '\\'"
                    + " OR town ILIKE :pattern ESCAPE '\\' OR use ILIKE :pattern ESCAPE '\\' OR slug ILIKE :pattern ESCAPE '\\'";
            conditions.add("(" + match + ")");
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sort = switch (order) {
            case "za" -> "lower(name) DESC, id DESC";
            case "recent" -> "id DESC";
            default -> "lower(name) ASC, id ASC";
        };
        if (!q.isEmpty() && "relevance".equals(order)) {
            sort = "CASE WHEN lower(name) = lower(:exact) THEN 0 "
                 + "WHEN name ILIKE :prefix ESCAPE '\\' THEN 1 "
                 + "WHEN name ILIKE :pattern ESCAPE '\\' THEN 2 ELSE 3 END, " + sort;
        }
        List<LocationDto> rows = jdbc.query("SELECT " + COLUMNS + " FROM " + from + where
                + " ORDER BY " + sort + " LIMIT :limit OFFSET :offset", params, MAPPER);
        Long total = jdbc.queryForObject("SELECT count(*) FROM " + from + where, params, Long.class);
        return PageResponse.of(rows, page, size, total == null ? 0 : total);
    }

    public Optional<LocationDto> findBySlug(String slug) {
        return jdbc.query("SELECT " + COLUMNS + " FROM locations WHERE slug = :slug",
                new MapSqlParameterSource("slug", slug), MAPPER).stream().findFirst();
    }
    public Optional<LocationDto> findByExternalId(long externalId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM locations WHERE external_id = :id",
                new MapSqlParameterSource("id", externalId), MAPPER).stream().findFirst();
    }
}
