package com.springfieldarchive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verificación de la migración V2 (seed catalog) contra PostgreSQL 18 real.
 * DB aislada; todas las pruebas se ejecutan tras V1 + V2 limpios desde cero.
 */
@SpringBootTest(properties = "spring.profiles.active=test", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestDatabaseContainer.class)
class CatalogSeedTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Flyway flyway;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void seedAppliedExactlyOnceWithNoDuplicates() {
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success AND version IN ('1','2')", Integer.class))
                .isEqualTo(2);
        // migrate() de nuevo no aplica nada adicional
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        // validate() confirma que el esquema coincide con las migraciones registradas
        org.assertj.core.api.Assertions.assertThatCode(() -> flyway.validate()).doesNotThrowAnyException();
    }

    @Test
    void seedCountsAreExact() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM characters", Integer.class)).isEqualTo(40);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM locations", Integer.class)).isEqualTo(20);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM (SELECT id FROM characters UNION ALL SELECT id FROM locations) t", Integer.class))
                .isEqualTo(60);
    }

    @Test
    void seedIntegrityConstraintsAreReal() {
        // unicidad de slugs
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM (SELECT slug FROM characters GROUP BY slug HAVING count(*) > 1"
                        + " UNION ALL SELECT slug FROM locations GROUP BY slug HAVING count(*) > 1) x", Integer.class))
                .isZero();
        // patrón y longitud
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM characters WHERE slug !~ '^[a-z0-9]+(-[a-z0-9]+)*$' OR char_length(slug) > 80", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM locations WHERE slug !~ '^[a-z0-9]+(-[a-z0-9]+)*$' OR char_length(slug) > 80", Integer.class))
                .isZero();
        // nada de cadenas vacías ni solo whitespace
        assertThat(jdbc.queryForObject("SELECT count(*) FROM characters WHERE name !~ '[^[:space:]]' OR description !~ '[^[:space:]]'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM locations  WHERE name !~ '[^[:space:]]' OR description !~ '[^[:space:]]'", Integer.class)).isZero();
        // occupation solo NULL o con caracteres visibles
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM characters WHERE occupation IS NOT NULL AND occupation !~ '[^[:space:]]'", Integer.class)).isZero();
        // estados y tipos cerrados
        assertThat(jdbc.queryForObject("SELECT count(*) FROM characters WHERE status NOT IN ('Alive','Deceased','Unknown')", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM locations  WHERE type   NOT IN ('home','business','landmark','school','park','plant')", Integer.class)).isZero();
        // LONGITUDES
        assertThat(jdbc.queryForObject("SELECT count(*) FROM characters WHERE char_length(name) > 120 OR char_length(description) > 2000 OR char_length(occupation) > 255", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM locations  WHERE char_length(name) > 120 OR char_length(description) > 2000", Integer.class)).isZero();
    }

    @Test
    void editorialArchiveStillServesSeedWithDeterministicOrderAndSearch() throws Exception {
        // El archivo editorial histórico es accesible explícitamente, pero no se confunde con la API.
        // 40 personajes en páginas de 20 -> 2 páginas, primera página: los 20 primeros ordenados
        List<String> slugsInOrder = new ArrayList<>();
        for (int p = 0; p < 2; p++) {
            HttpResult r = get("/api/v1/characters?source=editorial&size=20&page=" + p);
            assertThat(r.status()).isEqualTo(200);
            JsonNode root = json.readTree(r.body());
            assertThat(root.get("totalElements").asLong()).isEqualTo(40);
            assertThat(root.get("totalPages").asInt()).isEqualTo(2);
            for (JsonNode item : root.get("content")) {
                slugsInOrder.add(item.get("slug").asText());
            }
        }
        // El orden de llegada coincide exactamente con ORDER BY lower(name) ASC, id ASC en SQL
        List<String> expected = jdbc.query(
                "SELECT slug FROM characters ORDER BY lower(name) ASC, id ASC",
                (rs, rowNum) -> rs.getString("slug"));
        assertThat(slugsInOrder).containsExactlyElementsOf(expected);

        // 20 lugares visibles en una página de 20
        HttpResult locs = get("/api/v1/locations?source=editorial&size=20&page=0");
        assertThat(locs.status()).isEqualTo(200);
        JsonNode lp = json.readTree(locs.body());
        assertThat(lp.get("totalElements").asLong()).isEqualTo(20);
        assertThat(lp.get("content")).hasSize(20);

        // Búsqueda literal con los datos cargados
        assertThat(slugPresent(get("/api/v1/characters?source=editorial&q=homer").body(), "homer-simpson")).isTrue();
        assertThat(slugPresent(get("/api/v1/characters?source=editorial&q=mona").body(), "mona-simpson")).isTrue();
        assertThat(slugPresent(get("/api/v1/locations?source=editorial&q=Kwik").body(),  "kwik-e-mart")).isTrue();
        assertThat(slugPresent(get("/api/v1/locations?source=editorial&q=742").body(),    "simpson-house")).isTrue();
        assertThat(slugPresent(get("/api/v1/locations?source=editorial&q=escuela").body(), "springfield-elementary")).isTrue();

        // Ficha existente y 404
        HttpResult homer = get("/api/v1/characters/homer-simpson");
        assertThat(homer.status()).isEqualTo(200);
        assertThat(json.readTree(homer.body()).get("name").asText()).isEqualTo("Homer Simpson");

        HttpResult springfield = get("/api/v1/characters/no-existe-nada");
        assertThat(springfield.status()).isEqualTo(404);

        HttpResult house = get("/api/v1/locations/simpson-house");
        assertThat(house.status()).isEqualTo(200);
        assertThat(json.readTree(house.body()).get("type").asText()).isEqualTo("home");

        HttpResult house404 = get("/api/v1/locations/no-existe-nada");
        assertThat(house404.status()).isEqualTo(404);
    }

    private record HttpResult(int status, String body) {
    }

    private HttpResult get(String pathWithQuery) throws Exception {
        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + pathWithQuery)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        return new HttpResult(resp.statusCode(), resp.body());
    }

    private static boolean slugPresent(String body, String slug) throws Exception {
        JsonNode root = new ObjectMapper().readTree(body);
        for (JsonNode item : root.get("content")) {
            if (slug.equals(item.get("slug").asText())) {
                return true;
            }
        }
        return false;
    }
}
