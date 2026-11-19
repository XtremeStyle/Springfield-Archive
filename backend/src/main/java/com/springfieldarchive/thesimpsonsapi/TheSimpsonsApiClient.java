package com.springfieldarchive.thesimpsonsapi;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Optional;

/**
 * Cliente HTTP para The Simpsons API (https://thesimpsonsapi.com).
 * Sin autenticación. Sin llamadas masivas: los métodos consultan
 * una página o un recurso cada vez; el llamador decide cuándo y cuántas.
 * 404 en detalle devuelve Optional.empty(); otros fallos lanzan
 * ExternalApiException con un mensaje saneado (sin eco del cuerpo).
 */
@EnableConfigurationProperties(TheSimpsonsApiProperties.class)
@Component
public class TheSimpsonsApiClient {

    private final TheSimpsonsApiProperties props;
    private final ObjectMapper json;
    private final HttpClient http;
    private final URI baseUri;

    public TheSimpsonsApiClient(TheSimpsonsApiProperties props, ObjectMapper json) {
        this.props = props;
        this.json = json;
        HttpResponseSecurity.validateTimeout(props.getConnectTimeout(), "connect-timeout");
        HttpResponseSecurity.validateTimeout(props.getReadTimeout(), "read-timeout");
        this.baseUri = HttpResponseSecurity.validatedBaseUrl(props.getBaseUrl());
        this.http = HttpClient.newBuilder()
                .connectTimeout(props.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public ExternalPage<ExternalCharacter> characters(int page) {
        requirePositivePage(page);
        return fetchPage("/characters?page=" + page,
                new TypeReference<ExternalPage<ExternalCharacter>>() {
                });
    }

    public Optional<ExternalCharacter> character(long id) {
        requirePositiveId(id);
        return fetchOne("/characters/" + id, ExternalCharacter.class);
    }

    public ExternalPage<ExternalEpisode> episodes(int page) {
        requirePositivePage(page);
        return fetchPage("/episodes?page=" + page,
                new TypeReference<ExternalPage<ExternalEpisode>>() {
                });
    }

    public Optional<ExternalEpisode> episode(long id) {
        requirePositiveId(id);
        return fetchOne("/episodes/" + id, ExternalEpisode.class);
    }

    public ExternalPage<ExternalLocation> locations(int page) {
        requirePositivePage(page);
        return fetchPage("/locations?page=" + page,
                new TypeReference<ExternalPage<ExternalLocation>>() {
                });
    }

    public Optional<ExternalLocation> location(long id) {
        requirePositiveId(id);
        return fetchOne("/locations/" + id, ExternalLocation.class);
    }

    private static void requirePositivePage(int page) {
        if (page < 1 || page > 10000) { throw new IllegalArgumentException("Página externa fuera de rango"); }
    }

    private static void requirePositiveId(long id) {
        if (id < 1 || id > Integer.MAX_VALUE) { throw new IllegalArgumentException("ID externo fuera de rango"); }
    }

    private <T> ExternalPage<T> fetchPage(String path, TypeReference<ExternalPage<T>> type) {
        Response response = get(path);
        // Un 404 de listado NO equivale a una página vacía: ocultaría fallos
        // de la API externa y permitiría declarar una sincronización completa.
        if (response.statusCode() != 200) {
            throw new ExternalApiException(response.statusCode(), "respuesta no exitosa");
        }
        try {
            ExternalPage<T> page = json.readValue(response.body(), type);
            if (page == null || page.results() == null || page.count() < 0 || page.pages() < 0
                    || (page.count() > 0 && page.pages() == 0)
                    || (page.count() > 0 && page.results().isEmpty())) {
                throw new ExternalApiException("página inválida devuelta por la API externa");
            }
            return page;
        } catch (RuntimeException e) {
            throw new ExternalApiException("cuerpo inesperado de la API externa (HTTP 200)", e);
        }
    }

    private <T> Optional<T> fetchOne(String path, Class<T> type) {
        Response response = get(path);
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        if (response.statusCode() != 200) {
            throw new ExternalApiException(response.statusCode(), "respuesta no exitosa");
        }
        try {
            T value = json.readValue(response.body(), type);
            if (value == null) { throw new ExternalApiException("ficha externa vacía (HTTP 200)"); }
            return Optional.of(value);
        } catch (RuntimeException e) {
            throw new ExternalApiException("cuerpo inesperado de la API externa (HTTP 200)", e);
        }
    }

    private Response get(String path) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUri.toString().replaceAll("/+$", "") + path))
                .timeout(props.getReadTimeout())
                .header("Accept", "application/json")
                .GET()
                .build();
        final HttpResponse<InputStream> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new ExternalApiException("timeout de lectura contra la API externa", e);
        } catch (IOException e) {
            throw new ExternalApiException("error de conexión contra la API externa", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException("petición a la API externa interrumpida", e);
        }
        // BodyHandlers.ofInputStream entrega el stream después de los encabezados:
        // el timeout del request no garantiza por sí solo un límite para el cuerpo.
        // Lectura acotada en un hilo virtual, con deadline independiente.
        try (InputStream body = response.body()) {
            FutureTask<String> readTask = new FutureTask<>(() -> HttpResponseSecurity.readLimitedJson(body));
            Thread.ofVirtual().name("springfield-json-read").start(readTask);
            try {
                String jsonBody = readTask.get(Math.max(1, props.getReadTimeout().toMillis()), TimeUnit.MILLISECONDS);
                return new Response(response.statusCode(), jsonBody);
            } catch (TimeoutException e) {
                readTask.cancel(true);
                throw new ExternalApiException("tiempo de lectura agotado para la respuesta externa", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof HttpResponseSecurity.ResponseTooLargeException) {
                    throw new ExternalApiException("respuesta JSON externa demasiado grande", cause);
                }
                throw new ExternalApiException("fallo al leer la respuesta de la API externa", cause);
            } catch (InterruptedException e) {
                readTask.cancel(true);
                Thread.currentThread().interrupt();
                throw new ExternalApiException("lectura externa interrumpida", e);
            }
        } catch (IOException e) {
            throw new ExternalApiException("error al cerrar respuesta externa", e);
        }
    }

    private record Response(int statusCode, String body) {
    }
}
