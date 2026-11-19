package com.springfieldarchive.media;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class MediaProxyControllerTest {
    private static final byte[] SAMPLE_WEBP = { 'R','I','F','F', 6,0,0,0, 'W','E','B','P', 0,0 };

    @Test void rejectsUnknownResourceWithoutCallingUpstream() {
        MediaProxyController proxy = new MediaProxyController();
        assertEquals(HttpStatus.NOT_FOUND, proxy.image("evil", 1).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, proxy.image("character", 0).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, proxy.image("character", 100001).getStatusCode());
        assertFalse(MediaProxyController.looksLikeWebp("<html>".getBytes()));
        assertTrue(MediaProxyController.looksLikeWebp(SAMPLE_WEBP));
    }

    @Test void servesOnlyWebpFromStrictLocalFixture() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/500/character/1.webp", e -> {
            e.getResponseHeaders().add("Content-Type","image/webp");
            e.sendResponseHeaders(200,SAMPLE_WEBP.length);
            try (var body = e.getResponseBody()) { body.write(SAMPLE_WEBP); }
        });
        server.createContext("/500/character/2.webp", e -> {
            byte[] malicious = "<html>wrong content</html>".getBytes();
            e.getResponseHeaders().add("Content-Type","text/html");
            e.sendResponseHeaders(200,malicious.length);
            try (var body = e.getResponseBody()) { body.write(malicious); }
        });
        server.start();
        try {
            var origin=URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/");
            var http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();
            MediaProxyController controller = new MediaProxyController(origin,http);
            var response=controller.image("character",1);
            assertEquals(HttpStatus.OK,response.getStatusCode());
            assertArrayEquals(SAMPLE_WEBP,response.getBody());
            assertEquals(HttpStatus.BAD_GATEWAY,controller.image("character",2).getStatusCode());
            assertEquals(HttpStatus.NOT_FOUND,controller.image("character",3).getStatusCode());
        } finally { server.stop(0); }
    }
}
