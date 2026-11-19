package com.springfieldarchive.thesimpsonsapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuración del cliente para The Simpsons API.
 * Sin credenciales: la API es pública y anónima.
 */
@ConfigurationProperties(prefix = "thesimpsonsapi")
public class TheSimpsonsApiProperties {

    /** URL base de la API, incluida la ruta /api (sin barra final). */
    private String baseUrl = "https://thesimpsonsapi.com/api";

    /** Timeout para establecer la conexión TCP/TLS. */
    private Duration connectTimeout = Duration.ofSeconds(2);

    /** Timeout de lectura por petición. */
    private Duration readTimeout = Duration.ofSeconds(5);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
