package com.springfieldarchive.thesimpsonsapi;

import tools.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas del cliente contra un servidor HTTP embebido (JDK).
 * Sin Internet. Cada contexto de la ruta responde de forma determinista,
 * incluyendo contadores de pruebas para 500 e inválidos.
 */
class TheSimpsonsApiClientTest {

    private HttpServer server;
    private int port;
    private TheSimpsonsApiClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();

        TheSimpsonsApiProperties props = new TheSimpsonsApiProperties();
        props.setBaseUrl("http://127.0.0.1:" + port);
        props.setConnectTimeout(Duration.ofSeconds(2));
        props.setReadTimeout(Duration.ofSeconds(2));
        client = new TheSimpsonsApiClient(props, new ObjectMapper());

        server.createContext("/characters/1", exchange -> respond(exchange, 200,
                """
                {"id":1,"age":39,"birthdate":"1956-05-12",
                 "description":"Homer is a nuclear safety inspector.",
                 "first_appearance_ep_id":1,"first_appearance_sh_id":1,
                 "first_appearance_ep":{"id":1,"airdate":"1989-12-17","episode_number":1,
                   "image_path":"/episode/1.webp","name":"Simpsons Roasting on an Open Fire",
                   "season":1,"synopsis":"Season one opener.","description":"Episode description."},
                 "first_appearance_sh":{"id":1,"airdate":"1987-04-19","episode_number":1,
                   "image_path":"/short/1.webp","name":"Good Night","season":1,
                   "synopsis":"The short.","description":"Short description."},
                 "gender":"Male","name":"Homer Simpson","occupation":"Safety Inspector",
                 "phrases":["Doh!"],"portrait_path":"/character/1.webp","status":"Alive"}
                """));
        server.createContext("/characters", exchange -> respond(exchange, 200,
                """
                {"count":2,"pages":1,"next":null,"prev":null,"results":[
                  {"id":1,"age":39,"birthdate":"1956-05-12","gender":"Male","name":"Homer Simpson",
                   "occupation":"Safety Inspector","phrases":["Doh!"],"portrait_path":"/character/1.webp","status":"Alive"},
                  {"id":2,"age":39,"birthdate":null,"gender":"Female","name":"Marge Simpson",
                   "occupation":"Homemaker","phrases":["Hrmmm..."],"portrait_path":"/character/2.webp","status":"Alive"}
                ]}
                """));
        server.createContext("/episodes/1", exchange -> respond(exchange, 200,
                """
                {"id":1,"airdate":"1989-12-17","episode_number":1,"image_path":"/episode/1.webp",
                 "name":"Simpsons Roasting on an Open Fire","season":1,
                 "synopsis":"Season one opener.","description":"Episode description."}
                """));
        server.createContext("/episodes", exchange -> respond(exchange, 200,
                """
                {"count":1,"pages":1,"next":null,"prev":null,"results":[
                  {"id":1,"airdate":"1989-12-17","episode_number":1,"image_path":"/episode/1.webp",
                   "name":"Simpsons Roasting on an Open Fire","season":1,
                   "synopsis":"Season one opener."}
                ]}
                """));
        server.createContext("/locations/1", exchange -> respond(exchange, 200,
                """
                {"id":1,"description":"Home of the Simpson family.",
                 "first_appearance_ep_id":null,"first_appearance_sh_id":null,
                 "first_appearance_ep":null,"first_appearance_sh":null,
                 "image_path":"/location/1.webp","name":"742 Evergreen Terrace",
                 "town":"Springfield","use":"Residential"}
                """));
        server.createContext("/locations", exchange -> respond(exchange, 200,
                """
                {"count":1,"pages":1,"next":null,"prev":null,"results":[
                  {"id":1,"name":"742 Evergreen Terrace","image_path":"/location/1.webp",
                   "town":"Springfield","use":"Residential"}
                ]}
                """));
        server.createContext("/404/characters", exchange -> respond(exchange, 404, "{}"));
        server.createContext("/404/episodes", exchange -> respond(exchange, 404, "{}"));
        server.createContext("/404/locations", exchange -> respond(exchange, 404, "{}"));
        server.createContext("/fail", exchange -> respond(exchange, 500,
                "{\"error\":\"boom internal-marker no-deberia-aparecer\"}"));
        server.createContext("/broken", exchange -> respond(exchange, 200, "not-json"));
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
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

    private TheSimpsonsApiClient withUrl(String url) {
        TheSimpsonsApiProperties p = new TheSimpsonsApiProperties();
        p.setBaseUrl(url);
        p.setConnectTimeout(Duration.ofSeconds(2));
        p.setReadTimeout(Duration.ofSeconds(2));
        return new TheSimpsonsApiClient(p, new ObjectMapper());
    }

    @Test
    void listCharactersParsesEnvelopeAndNullableFields() {
        ExternalPage<ExternalCharacter> page = client.characters(1);

        assertThat(page.count()).isEqualTo(2);
        assertThat(page.pages()).isEqualTo(1);
        assertThat(page.next()).isNull();
        assertThat(page.prev()).isNull();
        assertThat(page.results()).hasSize(2);
        assertThat(page.results().get(0).name()).isEqualTo("Homer Simpson");
        assertThat(page.results().get(0).phrases()).containsExactly("Doh!");
        assertThat(page.results().get(0).birthdate()).isEqualTo("1956-05-12");
        assertThat(page.results().get(1).birthdate()).isNull();
    }

    @Test
    void characterDetailParsesNestedFirstAppearances() {
        ExternalCharacter homer = client.character(1).orElseThrow();

        assertThat(homer.name()).isEqualTo("Homer Simpson");
        assertThat(homer.description()).contains("nuclear");
        assertThat(homer.firstAppearanceEpId()).isEqualTo(1L);
        assertThat(homer.firstAppearanceShId()).isEqualTo(1L);
        assertThat(homer.firstAppearanceEp().name()).isEqualTo("Simpsons Roasting on an Open Fire");
        assertThat(homer.firstAppearanceEp().season()).isEqualTo(1);
        assertThat(homer.firstAppearanceSh().name()).isEqualTo("Good Night");
    }

    @Test
    void episodeListAndDetailParseFromList() {
        ExternalPage<ExternalEpisode> page = client.episodes(1);
        assertThat(page.results()).hasSize(1);
        assertThat(page.results().get(0).synopsis()).isEqualTo("Season one opener.");

        ExternalEpisode ep = client.episode(1).orElseThrow();
        assertThat(ep.description()).isEqualTo("Episode description.");
        assertThat(ep.airdate()).isEqualTo("1989-12-17");
    }

    @Test
    void locationListAndDetailParseNullables() {
        ExternalPage<ExternalLocation> page = client.locations(1);
        assertThat(page.results().get(0).town()).isEqualTo("Springfield");
        assertThat(page.results().get(0).use()).isEqualTo("Residential");

        ExternalLocation loc = client.location(1).orElseThrow();
        assertThat(loc.description()).isEqualTo("Home of the Simpson family.");
        assertThat(loc.firstAppearanceEp()).isNull();
        assertThat(loc.firstAppearanceSh()).isNull();
        assertThat(loc.firstAppearanceEpId()).isNull();
    }

    @Test
    void notFoundDetailsReturnOptionalEmpty() {
        TheSimpsonsApiClient notFoundClient = withUrl("http://127.0.0.1:" + port + "/404");
        assertThat(notFoundClient.character(1)).isEmpty();
        assertThat(notFoundClient.episode(1)).isEmpty();
        assertThat(notFoundClient.location(1)).isEmpty();
    }

    @Test
    void serverErrorThrowsSanitizedExternalApiException() {
        TheSimpsonsApiClient failing = withUrl("http://127.0.0.1:" + port + "/fail");

        assertThatThrownBy(() -> failing.characters(1))
                .isInstanceOf(ExternalApiException.class)
                .satisfies(ex -> assertThat(((ExternalApiException) ex).statusCode()).isEqualTo(500))
                .satisfies(ex -> assertThat(ex.getMessage())
                        .contains("HTTP 500")
                        .doesNotContain("internal-marker", "no-deberia-aparecer"));

        assertThatThrownBy(() -> failing.character(1))
                .isInstanceOf(ExternalApiException.class)
                .satisfies(ex -> assertThat(ex.getMessage())
                        .doesNotContain("internal-marker", "no-deberia-aparecer"));
    }

    @Test
    void invalidJsonThrowsExternalApiException() {
        TheSimpsonsApiClient failing = withUrl("http://127.0.0.1:" + port + "/broken");

        assertThatThrownBy(() -> failing.characters(1))
                .isInstanceOf(ExternalApiException.class)
                .extracting(Throwable::getMessage)
                .asString()
                .contains("cuerpo inesperado");
    }

    @Test
    void connectionRefusedThrowsExternalApiException() {
        int unusedPort;
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            unusedPort = s.getLocalPort();
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        TheSimpsonsApiClient refused = withUrl("http://127.0.0.1:" + unusedPort);
        assertThatThrownBy(() -> refused.characters(1))
                .isInstanceOf(ExternalApiException.class)
                .extracting(Throwable::getMessage)
                .asString()
                .contains("conexión");
    }


    @Test
    void missingListMustFailRatherThanLookEmpty() {
        TheSimpsonsApiClient missing = withUrl("http://127.0.0.1:" + port + "/404");
        assertThatThrownBy(() -> missing.characters(1))
                .isInstanceOf(ExternalApiException.class)
                .satisfies(ex -> assertThat(((ExternalApiException) ex).statusCode()).isEqualTo(404));
    }

    @Test
    void remoteJsonCannotExhaustTheProcessMemory() {
        server.createContext("/too-large/characters", exchange -> respond(exchange, 200,
                "x".repeat(HttpResponseSecurity.MAX_JSON_BYTES + 1)));
        TheSimpsonsApiClient limited = withUrl("http://127.0.0.1:" + port + "/too-large");
        assertThatThrownBy(() -> limited.characters(1))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("demasiado grande");
    }

    @Test
    void externalHttpRedirectIsNotFollowed() {
        server.createContext("/redirect/characters", exchange -> {
            exchange.getResponseHeaders().add("Location", "https://invalid.example/api/characters");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        TheSimpsonsApiClient limited = withUrl("http://127.0.0.1:" + port + "/redirect");
        assertThatThrownBy(() -> limited.characters(1))
                .isInstanceOf(ExternalApiException.class)
                .satisfies(ex -> assertThat(((ExternalApiException)ex).statusCode()).isEqualTo(302));
    }

    @Test
    void propertiesDefaultsAreSane() {
        TheSimpsonsApiProperties defaults = new TheSimpsonsApiProperties();
        assertThat(defaults.getBaseUrl()).isEqualTo("https://thesimpsonsapi.com/api");
        assertThat(defaults.getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(defaults.getReadTimeout()).isEqualTo(Duration.ofSeconds(5));
    }
}
