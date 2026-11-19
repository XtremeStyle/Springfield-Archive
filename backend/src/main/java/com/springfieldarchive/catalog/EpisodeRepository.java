package com.springfieldarchive.catalog;

import com.springfieldarchive.dto.EpisodeDto;
import com.springfieldarchive.dto.PageResponse;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class EpisodeRepository {

    private static final String COLUMNS =
            "id, slug, season, episode_number, airdate, name, synopsis, description, image_path, external_id, created_at";
    private static final RowMapper<EpisodeDto> MAPPER = (rs, rowNum) -> new EpisodeDto(
            rs.getLong("id"),
            (Integer) rs.getObject("external_id"),
            rs.getString("slug"),
            (Integer) rs.getObject("season"),
            (Integer) rs.getObject("episode_number"),
            rs.getString("airdate"),
            rs.getString("name"),
            rs.getString("synopsis"),
            rs.getString("description"),
            rs.getString("image_path"),
            rs.getObject("created_at", OffsetDateTime.class));

    public record SeasonSummary(int season, long episodes) { }
    private final NamedParameterJdbcTemplate jdbc;

    public EpisodeRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PageResponse<EpisodeDto> findPage(String q, Integer season, Integer episodeNumber, int page, int size) {
        return findPage(q, season, episodeNumber, "all", "chronological", page, size);
    }

    public PageResponse<EpisodeDto> findPage(String rawQ, Integer season, Integer episodeNumber,
                                              String scope, String order, int page, int size) {
        CatalogQueryValidator.requireChoice("scope", scope, "all", "title");
        CatalogQueryValidator.requireChoice("order", order, "chronological", "recent", "title");
        List<String> whereParts = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", size)
                .addValue("offset", page * size);
        String q = rawQ == null ? "" : rawQ.strip();
        if (!q.isEmpty()) {
            params.addValue("pattern", "%" + LikeEscape.escape(q) + "%");
            params.addValue("exact", q);
            params.addValue("prefix", LikeEscape.escape(q) + "%");
            String match = "name ILIKE :pattern ESCAPE '\\'";
            if ("all".equals(scope)) {
                match += " OR synopsis ILIKE :pattern ESCAPE '\\' OR description ILIKE :pattern ESCAPE '\\' OR slug ILIKE :pattern ESCAPE '\\'";
            }
            whereParts.add("(" + match + ")");
        }
        if (season != null) {
            whereParts.add("season = :season");
            params.addValue("season", season);
        }
        if (episodeNumber != null) {
            whereParts.add("episode_number = :number");
            params.addValue("number", episodeNumber);
        }
        String where = whereParts.isEmpty() ? "" : " WHERE " + String.join(" AND ", whereParts);
        // episode_number de The Simpsons API es global (por ejemplo el primer
        // episodio de la T2 tiene numero 14), NO el ordinal dentro de la T2.
        String sort = switch (order) {
            case "recent" -> "season DESC NULLS LAST, episode_number DESC NULLS LAST, id DESC";
            case "title" -> "lower(name) ASC, id ASC";
            default -> "season ASC NULLS LAST, episode_number ASC NULLS LAST, id ASC";
        };
        if (!q.isEmpty() && "title".equals(order)) {
            sort = "CASE WHEN lower(name) = lower(:exact) THEN 0 " +
                    "WHEN name ILIKE :prefix ESCAPE '\\' THEN 1 " +
                    "WHEN name ILIKE :pattern ESCAPE '\\' THEN 2 ELSE 3 END, " + sort;
        }
        List<EpisodeDto> content = jdbc.query(
                "SELECT " + COLUMNS + " FROM episodes" + where + " ORDER BY " + sort +
                        " LIMIT :limit OFFSET :offset", params, MAPPER);
        Long total = jdbc.queryForObject("SELECT count(*) FROM episodes" + where, params, Long.class);
        return PageResponse.of(content, page, size, total == null ? 0 : total);
    }

    /** Temporadas reales en PostgreSQL; no genera opciones sin episodios. */
    public List<SeasonSummary> findSeasons() {
        return jdbc.query("SELECT season, count(*) AS episodes FROM episodes " +
                        "WHERE season IS NOT NULL GROUP BY season ORDER BY season ASC",
                (rs, rowNum) -> new SeasonSummary(rs.getInt("season"), rs.getLong("episodes")));
    }

    public Optional<EpisodeDto> findBySlug(String slug) {
        List<EpisodeDto> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM episodes WHERE slug = :s",
                new MapSqlParameterSource().addValue("s", slug), MAPPER);
        return rows.stream().findFirst();
    }

    public Optional<EpisodeDto> findByExternalId(long externalId) {
        List<EpisodeDto> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM episodes WHERE external_id = :id",
                new MapSqlParameterSource().addValue("id", externalId), MAPPER);
        return rows.stream().findFirst();
    }
}
