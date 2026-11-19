package com.springfieldarchive.thesimpsonsapi;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpResponseSecurityTest {
    @Test
    void requiresHttpsExceptLoopbackTests() {
        assertThat(HttpResponseSecurity.validatedBaseUrl("https://thesimpsonsapi.com/api").getScheme()).isEqualTo("https");
        assertThat(HttpResponseSecurity.validatedBaseUrl("http://127.0.0.1:8089").getScheme()).isEqualTo("http");
        assertThatThrownBy(() -> HttpResponseSecurity.validatedBaseUrl("http://public.example/api"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HttpResponseSecurity.validatedBaseUrl("https://user:pass@example.org/api"))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test
    void enforcesJsonResponseSize() throws Exception {
        assertThat(HttpResponseSecurity.readLimitedJson(new ByteArrayInputStream("{}".getBytes()))).isEqualTo("{}");
        assertThatThrownBy(() -> HttpResponseSecurity.readLimitedJson(
                new ByteArrayInputStream(new byte[HttpResponseSecurity.MAX_JSON_BYTES+1])))
                .isInstanceOf(HttpResponseSecurity.ResponseTooLargeException.class);
    }
}
