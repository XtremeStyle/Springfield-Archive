import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.springfieldarchive.thesimpsonsapi.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import tools.jackson.databind.ObjectMapper;

/** Cliente HTTP real ante un servidor local simulado; requiere librerías Spring/Jackson en classpath. */
public class ClientContractSmoke {
    private static int passed;
    private static void check(boolean value, String name) { if (!value) throw new AssertionError(name); passed++; }
    private static void reply(HttpExchange exchange, int code, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, body.length);
        try (var out = exchange.getResponseBody()) { out.write(body); }
    }
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext("/api/characters", exchange -> {
                if (exchange.getRequestURI().getPath().endsWith("/77")) {
                    exchange.sendResponseHeaders(200, 1024);
                    try (var out = exchange.getResponseBody()) {
                        out.write("{\"id\":77,".getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        try { Thread.sleep(1400); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                        try { out.write("\"name\":\"Slow\"}".getBytes(StandardCharsets.UTF_8)); }
                        catch (IOException ignored) { /* cliente cerró el stream al agotar el tiempo */ }
                    }
                    return;
                }
                if (exchange.getRequestURI().getPath().endsWith("/1"))
                    reply(exchange, 200, "{\"id\":1,\"name\":\"Homer\",\"status\":\"Alive\"}");
                else reply(exchange, 200, "{\"count\":1,\"pages\":1,\"next\":null,\"prev\":null,\"results\":[{\"id\":1,\"name\":\"Homer\"}]}");
            });
            server.createContext("/api/episodes", exchange -> {
                exchange.getResponseHeaders().add("Location", "https://invalid.example/api/episodes");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            });
            server.createContext("/api/locations", exchange -> reply(exchange, 200, "a".repeat(HttpResponseSecurity.MAX_JSON_BYTES+1)));
            server.start();
            TheSimpsonsApiProperties props = new TheSimpsonsApiProperties();
            props.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api");
            props.setReadTimeout(Duration.ofSeconds(5));
            TheSimpsonsApiClient client = new TheSimpsonsApiClient(props, new ObjectMapper());
            check(client.characters(1).count()==1, "página JSON real");
            check(client.character(1).isPresent(), "detalle JSON real");
            try { client.episodes(1); throw new AssertionError("redirección seguida"); }
            catch (ExternalApiException ex) { check(ex.statusCode()==302, "no seguir redirecciones"); }
            try { client.locations(1); throw new AssertionError("respuesta demasiado grande aceptada"); }
            catch (ExternalApiException ex) { check(ex.getMessage().contains("demasiado grande"), "límite tamaño real"); }
            TheSimpsonsApiProperties slowProps = new TheSimpsonsApiProperties();
            slowProps.setBaseUrl(props.getBaseUrl());
            slowProps.setReadTimeout(Duration.ofMillis(200));
            TheSimpsonsApiClient slowClient = new TheSimpsonsApiClient(slowProps, new ObjectMapper());
            long started = System.nanoTime();
            try { slowClient.character(77); throw new AssertionError("Stream sin fecha límite"); }
            catch (ExternalApiException ex) { check(ex.getMessage().contains("tiempo de lectura"), "timeout de stream"); }
            check(Duration.ofNanos(System.nanoTime()-started).toMillis()<1100, "timeout oportuno de stream");
            System.out.println("OK: contrato HTTP/JSON cliente Java, " + passed + " verificaciones");
        } finally { server.stop(0); }
    }
}
