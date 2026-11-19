package com.springfieldarchive.stats;

import com.springfieldarchive.sync.SyncState;
import com.springfieldarchive.sync.SyncStateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Estadísticas", description = "Métricas públicas del catálogo")
public class StatsController {

    private final JdbcTemplate jdbc;
    private final SyncStateRepository syncStateRepository;

    public StatsController(JdbcTemplate jdbc, SyncStateRepository syncStateRepository) {
        this.jdbc = jdbc;
        this.syncStateRepository = syncStateRepository;
    }

    @GetMapping("/api/v1/stats")
    @Operation(summary = "Estadísticas generales", description = "Conteos totales y por recurso desde PostgreSQL local")
    @ApiResponse(responseCode = "200", description = "Estadísticas del catálogo")
    public Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> characters = new LinkedHashMap<>();
        characters.put("total", jdbc.queryForObject("""
                SELECT count(DISTINCT lower(regexp_replace(btrim(name), '[^[:alnum:]]+', '', 'g')))
                FROM characters WHERE external_id IS NOT NULL
                """, Integer.class));
        characters.put("editorial", jdbc.queryForObject("SELECT count(*) FROM characters WHERE external_id IS NULL", Integer.class));
        characters.put("alive", jdbc.queryForObject("SELECT count(*) FROM characters WHERE external_id IS NOT NULL AND status='Alive'", Integer.class));
        characters.put("deceased", jdbc.queryForObject("SELECT count(*) FROM characters WHERE external_id IS NOT NULL AND status='Deceased'", Integer.class));
        characters.put("unknown", jdbc.queryForObject("SELECT count(*) FROM characters WHERE external_id IS NOT NULL AND status='Unknown'", Integer.class));
        out.put("characters", characters);

        Map<String, Object> episodes = new LinkedHashMap<>();
        episodes.put("total", jdbc.queryForObject("SELECT count(*) FROM episodes", Integer.class));
        episodes.put("seasons", jdbc.queryForObject("SELECT count(DISTINCT season) FROM episodes WHERE season IS NOT NULL", Integer.class));
        out.put("episodes", episodes);

        Map<String, Object> locations = new LinkedHashMap<>();
        locations.put("total", jdbc.queryForObject("""
                SELECT count(DISTINCT lower(regexp_replace(btrim(name), '[^[:alnum:]]+', '', 'g')))
                FROM locations WHERE external_id IS NOT NULL
                """, Integer.class));
        locations.put("editorial", jdbc.queryForObject("SELECT count(*) FROM locations WHERE external_id IS NULL", Integer.class));
        locations.put("byType", jdbc.queryForList("SELECT type, count(*) AS count FROM locations WHERE external_id IS NOT NULL GROUP BY type ORDER BY type"));
        out.put("locations", locations);

        List<SyncState> states = syncStateRepository.findAll();
        out.put("sync", states.stream().map(s -> Map.of(
                "resource", s.resource(),
                "status", s.status(),
                "processed", s.processed(),
                "errors", s.errors(),
                "syncedAt", s.syncedAt() == null ? "" : s.syncedAt().toString()
        )).toList());
        return out;
    }

    @GetMapping("/api/v1/sync/status")
    @Operation(summary = "Estado de sincronización", description = "Estado de progreso de la importación desde la API externa")
    @ApiResponse(responseCode = "200", description = "Estado de los recursos de sincronización")
    public List<Map<String, Object>> syncStatus() {
        return syncStateRepository.findAll().stream().map(s -> Map.<String, Object>of(
                "resource", s.resource(),
                "status", s.status(),
                "lastPage", s.lastPage(),
                "processed", s.processed(),
                "errors", s.errors(),
                "syncedAt", s.syncedAt() == null ? "" : s.syncedAt().toString(),
                // El detalle crudo del error queda en el servidor; el público solo sabe si existe.
                "hasError", s.error() != null && !s.error().isBlank()
        )).toList();
    }
}
