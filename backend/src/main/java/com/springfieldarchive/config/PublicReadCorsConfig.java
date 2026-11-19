package com.springfieldarchive.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS opcional y restrictivo cuando Netlify llama directamente a Java.
 * Sin comodines y sin abrir POST /api/v1/sync/start al navegador remoto.
 */
@Configuration
public class PublicReadCorsConfig implements WebMvcConfigurer {
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/characters", "/api/v1/characters/**",
            "/api/v1/locations", "/api/v1/locations/**",
            "/api/v1/episodes", "/api/v1/episodes/**",
            "/api/v1/media/**", "/api/v1/stats", "/api/v1/sync/status"
    };
    private final String allowedOrigin;

    public PublicReadCorsConfig(@Value("${springfield.cors.allowed-origin:${SPRINGFIELD_CORS_ALLOWED_ORIGIN:}}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin == null ? "" : allowedOrigin.trim();
        if (!this.allowedOrigin.isEmpty() && !isSafePublicOrigin(this.allowedOrigin)) {
            throw new IllegalArgumentException("El origen CORS debe ser un origen HTTPS completo sin ruta");
        }
    }

    static boolean isSafePublicOrigin(String value) {
        try {
            URI u = URI.create(value);
            String host = u.getHost();
            return "https".equalsIgnoreCase(u.getScheme()) && host != null && !host.isBlank()
                    && u.getUserInfo() == null && u.getPort() == -1
                    && (u.getRawPath() == null || u.getRawPath().isEmpty())
                    && u.getRawQuery() == null && u.getRawFragment() == null
                    && host.matches("(?i)(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")
                    && !host.matches("[0-9.]+")
                    && !host.matches("(?i).*\\.(?:local|internal|invalid|test)");
        } catch (IllegalArgumentException e) { return false; }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigin.isEmpty()) return;
        for (String path : PUBLIC_PATHS) {
            registry.addMapping(path)
                    .allowedOrigins(allowedOrigin)
                    .allowedMethods("GET", "HEAD", "OPTIONS")
                    .allowedHeaders("Accept", "Content-Type")
                    .allowCredentials(false)
                    .maxAge(3600);
        }
    }
}
