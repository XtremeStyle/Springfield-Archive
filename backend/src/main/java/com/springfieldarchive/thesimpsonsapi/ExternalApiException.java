package com.springfieldarchive.thesimpsonsapi;

/**
 * Fallo controlado al hablar con la API externa:
 * estado HTTP no esperado, conexión imposible/timeout, o cuerpo no parseable.
 * El mensaje trunca cualquier cuerpo de respuesta para no exponer información.
 */
public class ExternalApiException extends RuntimeException {

    private final int statusCode;

    public ExternalApiException(String message) {
        super(message);
        this.statusCode = -1;
    }

    public ExternalApiException(int statusCode, String message) {
        super("HTTP " + statusCode + ": " + message);
        this.statusCode = statusCode;
    }

    public ExternalApiException(int statusCode, String message, Throwable cause) {
        super("HTTP " + statusCode + ": " + message, cause);
        this.statusCode = statusCode;
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public int statusCode() {
        return statusCode;
    }
}
