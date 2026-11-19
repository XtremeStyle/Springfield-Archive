package com.springfieldarchive;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica la importación contra PostgreSQL 18 real y un servidor HTTP simulado (JDK).
 * Nunca llama a Internet.
 */
@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "thesimpsonsapi.connect-timeout=PT1S",
        "thesimpsonsapi.read-timeout=PT1S"
})
@Import(TestDatabaseContainer.class)
class CatalogImportTest {

    private static HttpServer mockServer;
    private static int port;
    private static volatile boolean failLocationsPage2 = false;

    @BeforeAll
    static void startMockServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = mockServer.getAddress().getPort();

        // Personajes: list page 1 -> 1 personaje, detail id 101
        mockServer.createContext("/characters", exchange -> {
            String p = exchange.getRequestURI().getQuery();
            int page = p == null ? 1 : Integer.parseInt(p.replace("page=", ""));
            if (page == 1) {
                respond(exchange, 200, """
                        {"count":1,"pages":1,"next":null,"prev":null,"results":[
                          {"id":101,"age":39,"birthdate":"1956-05-12","gender":"Male","name":"Test Character",
                           "occupation":"Tester","phrases":["Testing!"],"portrait_path":"/character/101.webp","status":"Alive"}
                        ]}
                        """);
            } else {
                respond(exchange, 200, "{\"count\":1,\"pages\":1,\"next\":null,\"prev\":null,\"results\":[]}");
            }
        });
        mockServer.createContext("/characters/101", exchange -> respond(exchange, 200,
                """
                {"id":101,"age":39,"birthdate":"1956-05-12",
                 "description":"Importado desde TheSimpsons API (fixture local).",
                 "first_appearance_ep_id":null,"first_appearance_sh_id":null,
                 "first_appearance_ep":null,"first_appearance_sh":null,
                 "gender":"Male","name":"Test Character","occupation":"Tester",
                 "phrases":["Homer dice ||| aqui y mas ||| y mas aun","Saludo!"],"portrait_path":"/character/101.webp","status":"Alive"}
                """));
        // Episodios: 1 episodio, con detalle
        mockServer.createContext("/episodes", exchange -> respond(exchange, 200,
                """
                {"count":1,"pages":1,"next":null,"prev":null,"results":[
                  {"id":201,"airdate":"1990-01-01","episode_number":1,"image_path":"/episode/201.webp",
                   "name":"Test Episode","season":1,"synopsis":"A test episode."}
                ]}
                """));
        mockServer.createContext("/episodes/201", exchange -> respond(exchange, 200,
                """
                {"id":201,"airdate":"1990-01-01","episode_number":1,"image_path":"/episode/201.webp",
                 "name":"Test Episode","season":1,
                 "synopsis":"A test episode.","description":"Test description."}
                """));
        // Locations: page=2 falla la primara vez; se puede reactivar
        mockServer.createContext("/locations", exchange -> {
            String p = exchange.getRequestURI().getQuery();
            int page = p == null ? 1 : Integer.parseInt(p.replace("page=", ""));
            if (page == 1) {
                if (failLocationsPage2) {
                    respond(exchange, 500, "{\"error\":\"boom\"}");
                    return;
                }
                respond(exchange, 200, """
                        {"count":1,"pages":1,"next":null,"prev":null,"results":[
                          {"id":301,"name":"Test Location","image_path":"/location/301.webp",
                           "town":"Testville","use":"Residential"}
                        ]}
                        """);
            } else {
                respond(exchange, 200, "{\"count\":1,\"pages\":1,\"next\":null,\"prev\":null,\"results\":[]}");
            }
        });
        mockServer.createContext("/locations/301", exchange -> respond(exchange, 200,
                """
                {"id":301,"description":"Test location description.",
                 "first_appearance_ep_id":null,"first_appearance_sh_id":null,
                 "first_appearance_ep":null,"first_appearance_sh":null,
                 "image_path":"/location/301.webp","name":"Test Location",
                 "town":"Testville","use":"Residential"}
                """));
        mockServer.start();
    }

    @AfterAll
    static void stopMockServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void mockProps(DynamicPropertyRegistry registry) {
        registry.add("thesimpsonsapi.base-url", () -> "http://127.0.0.1:" + port);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) {
        try (OutputStream os = exchange.getResponseBody()) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            os.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private com.springfieldarchive.sync.ExternalImportService importer;

    @Autowired
    private com.springfieldarchive.sync.SyncStateRepository syncState;

    @Autowired
    private com.springfieldarchive.catalog.EpisodeRepository episodeRepository;

    @Autowired
    private com.springfieldarchive.sync.CatalogSyncWriter syncWriter;


    @org.junit.jupiter.api.BeforeEach
    void resetFixtures() {
        // Aislamiento absoluto: limpia toda la bandeja que este test usa
        jdbc.update("DELETE FROM episodes WHERE external_id = 201");
        jdbc.update("DELETE FROM characters WHERE external_id = 101");
        jdbc.update("DELETE FROM locations WHERE external_id = 301");
        jdbc.update("UPDATE sync_state SET status='idle', last_page=0, processed=0, errors=0,"
                + " synced_at=null, error=null WHERE resource IN ('characters','episodes','locations')");
        failLocationsPage2 = false;
    }

    @Test
    void importPersistsAndIsIdempotentAndResumable() {
        // Primera vuelta completo
        com.springfieldarchive.sync.ExternalImportService.ResourceResult importChars = importer.runResource("characters");
        assertThat(importChars.status()).isEqualTo("completed");
        assertThat(importChars.processed()).isEqualTo(1);

        com.springfieldarchive.sync.ExternalImportService.ResourceResult importEps = importer.runResource("episodes");
        assertThat(importEps.status()).isEqualTo("completed");
        assertThat(importEps.processed()).isEqualTo(1);

        com.springfieldarchive.sync.ExternalImportService.ResourceResult importLocs = importer.runResource("locations");
        assertThat(importLocs.status()).isEqualTo("completed");
        assertThat(importLocs.processed()).isEqualTo(1);

        // Segunda vuelta: no duplica
        importer.runResource("characters");
        importer.runResource("episodes");
        importer.runResource("locations");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM characters WHERE external_id = 101", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM episodes WHERE external_id = 201", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM locations WHERE external_id = 301", Integer.class)).isEqualTo(1);

        // El episodio importado es visible por su ID externo y por su slug
        assertThat(episodeRepository.findByExternalId(201)).isPresent();
        assertThat(episodeRepository.findBySlug("s1-e1-test-episode")).isPresent();

        // Simular una interrupción (registro fallido en locations) y reanudar
        jdbc.update("UPDATE sync_state SET status='failed', last_page=0, processed=0, errors=1 WHERE resource='locations'");
        failLocationsPage2 = false;
        com.springfieldarchive.sync.ExternalImportService.ResourceResult resumed = importer.runResource("locations");
        assertThat(resumed.status()).isEqualTo("completed");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM locations WHERE external_id = 301", Integer.class)).isEqualTo(1);
    }

    @Test
    void characterSyncEnrichesEditorialPortraitWithoutReplacingBiography() {
        // Registro editorial con el mismo nombre y sin imagen: caso real de V2.
        jdbc.update("INSERT INTO characters (slug, name, occupation, status, description) "
                + "VALUES ('test-character-editorial', 'Test Character', 'Historia local', 'Alive', 'Biografía editorial de prueba')");
        try {
            assertThat(importer.runResource("characters").status()).isEqualTo("completed");
            assertThat(jdbc.queryForObject(
                    "SELECT portrait_path FROM characters WHERE slug = 'test-character-editorial'",
                    String.class)).isEqualTo("/character/101.webp");
            assertThat(jdbc.queryForObject(
                    "SELECT description FROM characters WHERE slug = 'test-character-editorial'",
                    String.class)).isEqualTo("Biografía editorial de prueba");
            assertThat(jdbc.queryForObject(
                    "SELECT external_id FROM characters WHERE slug = 'test-character-editorial'",
                    Integer.class)).isNull();
        } finally {
            jdbc.update("DELETE FROM characters WHERE slug = 'test-character-editorial'");
        }
    }

    @Test
    void resyncDoesNotEraseExistingImagesWhenAnExternalFieldIsNull() {
        // IDs exclusivos de esta prueba: no dependemos del orden de otras importaciones.
        int charId = 99101, episodeId = 99201, locationId = 99301;
        try {
            syncWriter.upsertCharacter(charId, "Portrait retention test", "Tester", "Alive",
                    "Ficha de integración", 39, null, "Male", java.util.List.of("Hola"),
                    "/character/99101.webp", null, null);
            syncWriter.upsertEpisode(episodeId, 1, 201, "1990-01-01", "Episode retention test",
                    "Sinopsis", "Descripción", "/episode/99201.webp");
            syncWriter.upsertLocation(locationId, "Location retention test", "Springfield", "School",
                    "Descripción", "/location/99301.webp", null, null);

            syncWriter.upsertEpisode(episodeId, 1, 201, "1990-01-01", "Episode retention test",
                    "Sinopsis", "Descripción", null);
            syncWriter.upsertLocation(locationId, "Location retention test", "Springfield", "School",
                    "Descripción", null, null, null);
            syncWriter.upsertCharacter(charId, "Portrait retention test", "Tester", "Alive",
                    "Ficha de integración", 39, null, "Male", java.util.List.of("Hola"),
                    null, null, null);

            assertThat(jdbc.queryForObject("SELECT image_path FROM episodes WHERE external_id=99201", String.class))
                    .isEqualTo("/episode/99201.webp");
            assertThat(jdbc.queryForObject("SELECT image_path FROM locations WHERE external_id=99301", String.class))
                    .isEqualTo("/location/99301.webp");
            assertThat(jdbc.queryForObject("SELECT portrait_path FROM characters WHERE external_id=99101", String.class))
                    .isEqualTo("/character/99101.webp");
        } finally {
            jdbc.update("DELETE FROM characters WHERE external_id=99101");
            jdbc.update("DELETE FROM episodes WHERE external_id=99201");
            jdbc.update("DELETE FROM locations WHERE external_id=99301");
        }
    }

    @Test
    void importPreservesPhrasesWithDelimiterSequences() {
        importer.runResource("characters");

        String[] phrases = jdbc.query(
                "SELECT phrases FROM characters WHERE external_id = 101",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    java.sql.Array array = rs.getArray("phrases");
                    return array == null ? null : (String[]) array.getArray();
                });
        assertThat(phrases).isNotNull();
        assertThat(phrases).containsExactly("Homer dice · aqui y mas · y mas aun", "Saludo!");
    }

    @Test
    void importHandlesExternalServerFailureAndResumes() {
        failLocationsPage2 = true;

        com.springfieldarchive.sync.ExternalImportService.ResourceResult first = importer.runResource("locations");
        assertThat(first.status()).isEqualTo("failed");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM locations WHERE external_id = 301", Integer.class)).isZero();

        failLocationsPage2 = false;
        com.springfieldarchive.sync.ExternalImportService.ResourceResult second = importer.runResource("locations");
        assertThat(second.status()).isEqualTo("completed");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM locations WHERE external_id = 301", Integer.class)).isEqualTo(1);
    }
}
