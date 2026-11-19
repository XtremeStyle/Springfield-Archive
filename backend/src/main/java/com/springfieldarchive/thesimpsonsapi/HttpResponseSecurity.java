package com.springfieldarchive.thesimpsonsapi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Validación independiente del proveedor: únicamente HTTPS fuera de loopback,
 * sin credenciales embebidas, y máximo de bytes leídos antes de decodificar JSON.
 * No es un filtro de SQL ni una garantía de disponibilidad de terceros.
 */
public final class HttpResponseSecurity {
    public static final int MAX_JSON_BYTES = 2 * 1024 * 1024;

    private HttpResponseSecurity() { }

    public static URI validatedBaseUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("La URL del proveedor no está configurada");
        }
        URI uri;
        try { uri = URI.create(raw.trim()); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("URL de API externa inválida", ex); }
        if (uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("La URL externa no admite usuario, query ni fragmento");
        }
        String host = uri.getHost();
        boolean loopback = "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host) || "::1".equals(host) || "[::1]".equals(host);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                && !(loopback && "http".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("La API externa exige HTTPS fuera de loopback");
        }
        if (uri.getPort() > 65535 || uri.getPort() == 0) {
            throw new IllegalArgumentException("Puerto del proveedor inválido");
        }
        return uri;
    }

    public static void validateTimeout(Duration value, String field) {
        if (value == null || value.isNegative() || value.isZero() || value.compareTo(Duration.ofMinutes(1)) > 0) {
            throw new IllegalArgumentException(field + " debe estar entre 1 ms y 60 s");
        }
    }

    public static String readLimitedJson(InputStream stream) throws IOException {
        try (InputStream input = stream) {
            byte[] bytes = input.readNBytes(MAX_JSON_BYTES + 1);
            if (bytes.length > MAX_JSON_BYTES) {
                throw new ResponseTooLargeException();
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public static final class ResponseTooLargeException extends IOException {
        public ResponseTooLargeException() {
            super("La API externa superó el tamaño máximo permitido");
        }
    }
}
