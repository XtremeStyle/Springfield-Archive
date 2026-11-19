package com.springfieldarchive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica endpoints de estadísticas/sincronización/episodios sin Internet.
 */
@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "thesimpsonsapi.connect-timeout=PT1S",
        "thesimpsonsapi.read-timeout=PT1S",
        "springfield.admin.token=test_admin_token"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestDatabaseContainer.class)
class CatalogStatsSyncTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();

    private record HttpResult(int status, String body) {
    }

    private HttpResult get(String path) throws Exception {
        HttpResponse<String> resp = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        return new HttpResult(resp.statusCode(), resp.body());
    }

    private HttpResult post(String path, String body, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            b.header("X-Admin-Token", token);
        }
        HttpResponse<String> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new HttpResult(resp.statusCode(), resp.body());
    }

    @Test
    void statsReturnsCountsAndSyncList() throws Exception {
        HttpResult stats = get("/api/v1/stats");
        assertThat(stats.status()).isEqualTo(200);
        JsonNode body = json.readTree(stats.body());
        assertThat(body.path("characters").path("total").isNumber()).isTrue();
        assertThat(body.path("characters").path("alive").isNumber()).isTrue();
        assertThat(body.path("episodes").path("total").isNumber()).isTrue();
        assertThat(body.path("locations").path("total").isNumber()).isTrue();
        assertThat(body.path("sync").isArray()).isTrue();
    }

    @Test
    void syncStatusListsThreeResources() throws Exception {
        HttpResult r = get("/api/v1/sync/status");
        assertThat(r.status()).isEqualTo(200);
        JsonNode body = json.readTree(r.body());
        assertThat(body).hasSize(3);
        for (JsonNode item : body) {
            assertThat(item.path("resource").asText()).isIn("characters", "episodes", "locations");
            assertThat(item.path("status").asText()).isIn("idle", "running", "completed", "failed");
        }
    }

    @Test
    void syncStartWithoutTokenIsForbidden() throws Exception {
        HttpResult r = post("/api/v1/sync/start?resource=characters", "", null);
        assertThat(r.status()).isEqualTo(403);
        assertThat(r.body()).contains("forbidden");
    }

    @Test
    void syncStartWithWrongTokenIsForbidden() throws Exception {
        HttpResult r = post("/api/v1/sync/start?resource=characters", "", "wrong-token");
        assertThat(r.status()).isEqualTo(403);
    }

    @Test
    void syncStartWithBadResourceIsRejected() throws Exception {
        HttpResult r = post("/api/v1/sync/start?resource=not-a-resource", "", "test_admin_token");
        assertThat(r.status()).isEqualTo(400);
    }

    @Test
    void episodesEndpointsRespondWithExpectedShape() throws Exception {
        HttpResult list = get("/api/v1/episodes?size=5");
        assertThat(list.status()).isEqualTo(200);
        JsonNode body = json.readTree(list.body());
        assertThat(body.path("content").isArray()).isTrue();
        assertThat(body.path("totalPages").isInt()).isTrue();
        // Detalle por slug inexistente -> 404 estructurado
        HttpResult notFound = get("/api/v1/episodes/no-existe");
        assertThat(notFound.status()).isEqualTo(404);
        assertThat(notFound.body()).contains("title");
    }
}
