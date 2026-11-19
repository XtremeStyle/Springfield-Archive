package com.springfieldarchive.media;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Fallback de retratos del catálogo. Solo permite recursos WebP del CDN
 * oficial, nunca URLs arbitrarias proporcionadas por el cliente.
 *
 * Las páginas usan el CDN directamente; este endpoint únicamente se invoca
 * cuando una imagen falla en el navegador. Límite de tamaño, tiempo y
 * concurrencia para evitar convertir el servidor en un proxy abierto.
 */
@RestController
public class MediaProxyController {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final long MAX_ID = 100_000L;
    private static final Map<String, String> TYPES = Map.of(
            "character", "character", "episode", "episode", "location", "location", "short", "short");
    private static final URI CDN = URI.create("https://cdn.thesimpsonsapi.com/");
    private static final Duration TIMEOUT = Duration.ofSeconds(7);
    private final Semaphore concurrentDownloads = new Semaphore(6);
    private final URI trustedCdn;
    private final HttpClient client;

    /** Spring utiliza exclusivamente este constructor, con host remoto fijo. */
    public MediaProxyController() {
        this(CDN, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    /** Inyección limitada al paquete para pruebas con un servidor HTTP simulado. */
    MediaProxyController(URI trustedCdn, HttpClient client) {
        this.trustedCdn = trustedCdn;
        this.client = client;
    }

    @GetMapping(value = "/api/v1/media/{kind}/{id}", produces = "image/webp")
    public ResponseEntity<byte[]> image(@PathVariable String kind, @PathVariable long id) {
        String resource = TYPES.get(kind);
        if (resource == null || id < 1 || id > MAX_ID) {
            return ResponseEntity.notFound().build();
        }
        if (!concurrentDownloads.tryAcquire()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        try {
            // La URL no puede ser manipulada: host HTTPS y ruta fija + ID numérico.
            URI imageUri = trustedCdn.resolve("500/" + resource + "/" + id + ".webp");
            HttpRequest request = HttpRequest.newBuilder(imageUri)
                    .timeout(TIMEOUT)
                    .header("Accept", "image/webp")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() == 404) { return ResponseEntity.notFound().build(); }
                if (response.statusCode() != 200) {
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
                }
                String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase();
                if (!contentType.startsWith("image/webp")) {
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
                }
                if (response.headers().firstValueAsLong("content-length").orElse(0) > MAX_BYTES) {
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
                }
                FutureTask<byte[]> task = new FutureTask<>(() -> body.readNBytes(MAX_BYTES + 1));
                Thread.ofVirtual().name("springfield-image-read").start(task);
                try {
                    byte[] bytes = task.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                    if (bytes.length == 0 || bytes.length > MAX_BYTES || !looksLikeWebp(bytes)) {
                        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
                    }
                    return ResponseEntity.ok()
                            .cacheControl(CacheControl.maxAge(Duration.ofHours(24)).cachePublic())
                            .header("X-Content-Type-Options", "nosniff")
                            .contentType(MediaType.parseMediaType("image/webp"))
                            .body(bytes);
                } catch (TimeoutException e) {
                    task.cancel(true);
                    return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
                } catch (ExecutionException e) {
                    task.cancel(true);
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
                }
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } finally {
            concurrentDownloads.release();
        }
    }

    /** Verifica la firma RIFF/WEBP: nunca servir HTML con el MIME de una imagen. */
    static boolean looksLikeWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }
}
