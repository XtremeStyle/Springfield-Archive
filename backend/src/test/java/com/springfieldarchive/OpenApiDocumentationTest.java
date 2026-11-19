package com.springfieldarchive;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=test")
@Import(TestDatabaseContainer.class)
class OpenApiDocumentationTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Test
    void apiDocsExposeExpectedInfo() throws Exception {
        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).contains("Springfield Archive API");
        assertThat(resp.body()).contains("1.1.1");
    }

    @Test
    void swaggerUiIsReachableInTestProfile() throws Exception {
        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/swagger-ui.html")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        // springdoc puede responder 200 (contenido) o redirección al UI real
        assertThat(resp.statusCode()).isIn(200, 301, 302, 303, 307);
    }
}
