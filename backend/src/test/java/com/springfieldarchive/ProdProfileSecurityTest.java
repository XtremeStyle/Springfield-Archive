package com.springfieldarchive;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=prod")
@Import(TestDatabaseContainer.class)
class ProdProfileSecurityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private Environment environment;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Test
    void activeProfileIsExactlyProd() {
        assertThat(environment.getActiveProfiles()).containsExactly("prod");
    }

    @Test
    void openApiIsDisabledAndHealthStaysUp() throws Exception {
        HttpResponse<String> apiDocs = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(apiDocs.statusCode()).isEqualTo(404);

        HttpResponse<String> swaggerUi = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/swagger-ui.html")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(swaggerUi.statusCode()).isEqualTo(404);

        HttpResponse<String> health = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.body()).contains("\"status\":\"UP\"");
    }
}
