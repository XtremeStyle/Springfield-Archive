package com.springfieldarchive.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicReadCorsConfigTest {
    @Test
    void refusesUnsafeOrigins() {
        for (String invalid : new String[] {"http://site.example", "https://localhost", "https://127.0.0.1",
                "https://host.example/path", "https://user:pass@host.example", "https://host.example:8080",
                "https://host.example?token=secret", "https://host.example/#fragment", "https://192.168.1.10", "https://[::1]", "https://service.internal"}) {
            assertThat(PublicReadCorsConfig.isSafePublicOrigin(invalid)).as(invalid).isFalse();
            assertThatThrownBy(() -> new PublicReadCorsConfig(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void acceptsOnlyExactHttpsOriginAndCanDisableCors() {
        assertThat(PublicReadCorsConfig.isSafePublicOrigin("https://springfield.netlify.app")).isTrue();
        new PublicReadCorsConfig("").addCorsMappings(new CorsRegistry());
        new PublicReadCorsConfig("https://springfield.netlify.app").addCorsMappings(new CorsRegistry());
    }
}
