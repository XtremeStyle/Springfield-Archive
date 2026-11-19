package com.springfieldarchive.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Cabeceras defensivas para la aplicación servida por Spring Boot (mismo origen).
 * La CSP no permite scripts inline y solo admite imágenes del CDN oficial.
 * Los estilos se sirven desde CSS estático. No se autorizan estilos inline en CSP.
 * En Netlify estas cabeceras deben configurarse también en netlify.toml.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final Environment environment;

    public SecurityHeadersFilter(Environment environment) {
        this.environment = environment;
    }

    private static final String CSP = "default-src 'self'; "
            + "base-uri 'self'; object-src 'none'; frame-ancestors 'none'; "
            + "form-action 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data: https://cdn.thesimpsonsapi.com; "
            + "media-src 'self'; "
            + "connect-src 'self'; font-src 'self'";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy",
                request.getRequestURI().startsWith("/api/v1/media/") ? "cross-origin" : "same-origin");
        if (request.getRequestURI().startsWith("/api/v1/sync/")) {
            response.setHeader("Cache-Control", "no-store");
        }
        boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
        if (prod && request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000");
        }
        // Solo Swagger en desarrollo puede necesitar una CSP diferente.
        if (prod || (!request.getRequestURI().startsWith("/swagger-ui")
                && !request.getRequestURI().startsWith("/v3/api-docs"))) {
            response.setHeader("Content-Security-Policy", CSP);
        }
        filterChain.doFilter(request, response);
    }
}
