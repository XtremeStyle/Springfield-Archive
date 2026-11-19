import com.springfieldarchive.sync.AdminTokenVerifier;
import com.springfieldarchive.thesimpsonsapi.HttpResponseSecurity;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Prueba sin Maven de clases de seguridad realmente utilizadas por el backend. */
public class SecuritySmokeTest {
    private static int passed;
    private static void check(boolean condition, String label) {
        if (!condition) { throw new AssertionError(label); }
        passed++;
    }
    private static void bad(Runnable test, String label) {
        try { test.run(); throw new AssertionError("Aceptó: " + label); }
        catch (IllegalArgumentException expected) { passed++; }
    }
    public static void main(String[] args) throws Exception {
        String strong = "0123456789abcdef0123456789abcdef0123456789abcdef";
        check(AdminTokenVerifier.valid(strong, strong, true), "token prod fuerte");
        check(!AdminTokenVerifier.valid(strong, strong + "x", true), "token incorrecto");
        check(!AdminTokenVerifier.valid("short", "short", true), "token débil en prod");
        check(AdminTokenVerifier.valid("short", "short", false), "token local para pruebas");
        check(!AdminTokenVerifier.valid(null, strong, true), "token no configurado");
        check(!AdminTokenVerifier.valid(strong, null, true), "token ausente");
        check(!AdminTokenVerifier.valid(" ", " ", false), "blancos");
        check(!AdminTokenVerifier.valid("a".repeat(513), "a".repeat(513), false), "cabecera excesiva");
        URI https = HttpResponseSecurity.validatedBaseUrl("https://thesimpsonsapi.com/api");
        check("https".equals(https.getScheme()), "origen HTTPS");
        check("http".equals(HttpResponseSecurity.validatedBaseUrl("http://127.0.0.1:8765").getScheme()), "test loopback");
        bad(() -> HttpResponseSecurity.validatedBaseUrl("http://example.com/api"), "HTTP remoto");
        bad(() -> HttpResponseSecurity.validatedBaseUrl("https://u:p@example.com/api"), "credenciales en URL");
        bad(() -> HttpResponseSecurity.validatedBaseUrl("https://example.com/api?token=x"), "query en base");
        bad(() -> HttpResponseSecurity.validatedBaseUrl("file:///etc/passwd"), "protocolo file");
        bad(() -> HttpResponseSecurity.validateTimeout(Duration.ZERO, "timeout"), "timeout cero");
        bad(() -> HttpResponseSecurity.validateTimeout(Duration.ofMinutes(2), "timeout"), "timeout ilimitado");
        check("{}".equals(HttpResponseSecurity.readLimitedJson(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)))), "respuesta normal");
        check(HttpResponseSecurity.readLimitedJson(new ByteArrayInputStream(new byte[HttpResponseSecurity.MAX_JSON_BYTES])).length() == HttpResponseSecurity.MAX_JSON_BYTES, "límite exacto");
        try {
            HttpResponseSecurity.readLimitedJson(new ByteArrayInputStream(new byte[HttpResponseSecurity.MAX_JSON_BYTES + 1]));
            throw new AssertionError("Aceptó JSON sobredimensionado");
        } catch (HttpResponseSecurity.ResponseTooLargeException expected) { passed++; }
        System.out.println("OK seguridad Java pura: " + passed + " verificaciones");
    }
}
