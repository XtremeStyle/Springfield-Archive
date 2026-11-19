package com.springfieldarchive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración del catálogo contra PostgreSQL 18 (Testcontainers).
 * Datos con prefijo "h4-"; se limpian en cada @BeforeEach.
 */
@SpringBootTest(properties = "spring.profiles.active=test", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestDatabaseContainer.class)
class CatalogApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM characters WHERE slug LIKE 'h4-%'");
        jdbc.update("DELETE FROM episodes WHERE slug LIKE 'h4-%'");
        jdbc.update("DELETE FROM locations WHERE slug LIKE 'h4-%'");
    }

    @org.junit.jupiter.api.AfterEach
    void cleanAfter() {
        // Ninguna fila de prueba queda para otras clases (p. ej. CatalogSeedTest)
        clean();
    }

    private record HttpResult(int status, String body) {
    }

    private HttpResult get(String pathWithQuery) throws Exception {
        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + pathWithQuery)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        return new HttpResult(resp.statusCode(), resp.body());
    }

    private static String q(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void charRow(String slug, String name, String occupation, String status, String description) {
        jdbc.update("INSERT INTO characters(slug,name,occupation,status,description) VALUES(?,?,?,?,?)",
                slug, name, occupation, status, description);
    }

    private void locRow(String slug, String name, String type, String description) {
        jdbc.update("INSERT INTO locations(slug,name,type,description) VALUES(?,?,?,?)",
                slug, name, type, description);
    }

    // ---------- Listados y paginación ----------

    @Test
    void listCharactersPaginatesDeterministically() throws Exception {
        charRow("h4-c", "H4 Personaje C", "Ocupacion C", "Alive", "Desc C");
        charRow("h4-a", "H4 Personaje A", "Ocupacion A", "Alive", "Desc A");
        charRow("h4-b", "H4 Personaje B", "Ocupacion B", "Alive", "Desc B");

        HttpResult page0 = get("/api/v1/characters?source=editorial&q=h4&size=2&page=0");
        assertThat(page0.status()).isEqualTo(200);
        JsonNode body = json.readTree(page0.body());
        assertThat(body.get("totalElements").asLong()).isEqualTo(3);
        assertThat(body.get("totalPages").asInt()).isEqualTo(2);
        assertThat(body.get("size").asInt()).isEqualTo(2);
        assertThat(body.get("page").asInt()).isEqualTo(0);
        assertThat(body.get("first").asBoolean()).isTrue();
        assertThat(body.get("last").asBoolean()).isFalse();
        assertThat(body.get("content")).hasSize(2);
        assertThat(body.get("content").get(0).get("slug").asText()).isEqualTo("h4-a");
        assertThat(body.get("content").get(1).get("slug").asText()).isEqualTo("h4-b");

        HttpResult page1 = get("/api/v1/characters?source=editorial&q=h4&size=2&page=1");
        JsonNode body1 = json.readTree(page1.body());
        assertThat(body1.get("content")).hasSize(1);
        assertThat(body1.get("first").asBoolean()).isFalse();
        assertThat(body1.get("last").asBoolean()).isTrue();
        assertThat(body1.get("content").get(0).get("slug").asText()).isEqualTo("h4-c");
    }

    @Test
    void listLocationsPaginatesDeterministically() throws Exception {
        locRow("h4-lb", "H4 Barrio B", "landmark", "Desc B");
        locRow("h4-la", "H4 Barrio A", "park", "Desc A");
        locRow("h4-lc", "H4 Barrio C", "home", "Desc C");

        HttpResult r = get("/api/v1/locations?source=editorial&q=h4&size=50&page=0");
        assertThat(r.status()).isEqualTo(200);
        JsonNode body = json.readTree(r.body());
        assertThat(body.get("totalElements").asLong()).isEqualTo(3);
        assertThat(body.get("content").get(0).get("slug").asText()).isEqualTo("h4-la");
        assertThat(body.get("content").get(1).get("slug").asText()).isEqualTo("h4-lb");
        assertThat(body.get("content").get(2).get("slug").asText()).isEqualTo("h4-lc");
    }

    @Test
    void emptyCatalogAndEmptyPageRespondWithoutErrors() throws Exception {
        // Sin coincidencias para el prefijo de búsqueda (página 0)
        HttpResult r = get("/api/v1/characters?source=editorial&q=zzz-no-match&page=0&size=10");
        assertThat(r.status()).isEqualTo(200);
        JsonNode body = json.readTree(r.body());
        assertThat(body.get("content")).isEmpty();
        assertThat(body.get("totalElements").asLong()).isZero();
        assertThat(body.get("totalPages").asInt()).isZero();
        assertThat(body.get("first").asBoolean()).isTrue();
        assertThat(body.get("last").asBoolean()).isTrue();
        // Página vacía más allá del total, pero con resultados existentes
        charRow("h4-only", "H4 Solo Uno", null, "Alive", "Desc unica");
        HttpResult beyond = get("/api/v1/characters?source=editorial&q=h4&page=5&size=10");
        assertThat(beyond.status()).isEqualTo(200);
        JsonNode b = json.readTree(beyond.body());
        assertThat(b.get("content")).isEmpty();
        assertThat(b.get("first").asBoolean()).isFalse();
        assertThat(b.get("last").asBoolean()).isTrue();
        assertThat(b.get("totalElements").asLong()).isEqualTo(1);
    }

    @Test
    void pageAndSizeDefaultsAndLimitsAreEnforced() throws Exception {
        charRow("h4-one", "H4 Uno", null, "Alive", "Desc uno");
        // Valores por defecto
        HttpResult def = get("/api/v1/characters?source=editorial&q=h4");
        JsonNode defBody = json.readTree(def.body());
        assertThat(defBody.get("page").asInt()).isZero();
        assertThat(defBody.get("size").asInt()).isEqualTo(20);
        // page=100 válido
        HttpResult maxPage = get("/api/v1/characters?source=editorial&q=h4&page=100");
        assertThat(maxPage.status()).isEqualTo(200);
        // size=50 válido
        HttpResult maxSize = get("/api/v1/characters?source=editorial&q=h4&size=50");
        assertThat(maxSize.status()).isEqualTo(200);
        // page=-1 -> 400
        assertThat(get("/api/v1/characters?source=editorial&q=h4&page=-1").status()).isEqualTo(400);
        // page=101 -> 400
        assertThat(get("/api/v1/characters?source=editorial&q=h4&page=101").status()).isEqualTo(400);
        // size=0 -> 400
        assertThat(get("/api/v1/characters?source=editorial&q=h4&size=0").status()).isEqualTo(400);
        // size=51 -> 400
        assertThat(get("/api/v1/characters?source=editorial&q=h4&size=51").status()).isEqualTo(400);
        // page no numérica -> 400
        assertThat(get("/api/v1/characters?source=editorial&q=h4&page=abc").status()).isEqualTo(400);
        // size no numérico -> 400
        assertThat(get("/api/v1/characters?source=editorial&q=h4&size=abc").status()).isEqualTo(400);
    }

    // ---------- Búsqueda literal ----------

    @Test
    void qSearchIsPartialCaseInsensitiveAcrossFields() throws Exception {
        charRow("h4-multi", "H4 Caso Especial", "Inspector NUCLEAR", "Alive", "Misterio del Roble");
        // case-insensitive en name
        JsonNode byName = json.readTree(get("/api/v1/characters?source=editorial&q=" + q("caso especial")).body());
        assertThat(containsSlug(byName, "h4-multi")).isTrue();
        // case-insensitive en occupation
        JsonNode byOcc = json.readTree(get("/api/v1/characters?source=editorial&q=" + q("NuCleAr")).body());
        assertThat(containsSlug(byOcc, "h4-multi")).isTrue();
        // case-insensitive en description
        JsonNode byDesc = json.readTree(get("/api/v1/characters?source=editorial&q=" + q("ROBLE")).body());
        assertThat(containsSlug(byDesc, "h4-multi")).isTrue();
        // sin coincidencias
        JsonNode none = json.readTree(get("/api/v1/characters?source=editorial&q=" + q("sin-coincidencia-zzz")).body());
        assertThat(none.get("totalElements").asLong()).isZero();
    }

    @Test
    void qTreatsPercentUnderscoreAndBackslashLiterally() throws Exception {
        charRow("h4-pct", "H4 Cien % Titulo", null, "Alive", "Desc pct");
        charRow("h4-plain", "H4 Cien x Titulo", null, "Alive", "Desc plain");
        charRow("h4-und", "H4 A_B Titulo", null, "Alive", "Desc und");
        charRow("h4-ab", "H4 AxB Titulo", null, "Alive", "Desc ab");
        charRow("h4-back", "H4 Barra \\ Literal", null, "Alive", "Desc back");

        JsonNode pct = json.readTree(get("/api/v1/characters?source=editorial&q=" + q("%")).body());
        assertThat(containsSlug(pct, "h4-pct")).isTrue();
        assertThat(containsSlug(pct, "h4-plain")).isFalse();

        JsonNode und = json.readTree(get("/api/v1/characters?source=editorial&q=" + q("_")).body());
        assertThat(containsSlug(und, "h4-und")).isTrue();
        assertThat(containsSlug(und, "h4-ab")).isFalse();

        JsonNode back = json.readTree(get("/api/v1/characters?source=editorial&q=" + q("\\")).body());
        assertThat(containsSlug(back, "h4-back")).isTrue();
        assertThat(containsSlug(back, "h4-plain")).isFalse();
    }

    @Test
    void qBlankActsAsNoFilter() throws Exception {
        charRow("h4-unique-token-xyz", "H4 Token Unico", null, "Alive", "Desc unique");
        HttpResult blank = get("/api/v1/characters?source=editorial&q=" + q("   "));
        assertThat(blank.status()).isEqualTo(200);
        JsonNode blankBody = json.readTree(blank.body());
        assertThat(containsSlug(blankBody, "h4-unique-token-xyz")).isTrue();

        // q vacío también actúa como ausencia de filtro
        HttpResult empty = get("/api/v1/characters?source=editorial&q=");
        JsonNode emptyBody = json.readTree(empty.body());
        assertThat(containsSlug(emptyBody, "h4-unique-token-xyz")).isTrue();
    }

    @Test
    void characterFiltersKeepEditorialAndImportedRowsSeparate() throws Exception {
        charRow("h4-ui-1", "H4 Investigador Unico", "Archivista", "Alive", "Pista museo-sombrero-9876");
        JsonNode editorial = json.readTree(get("/api/v1/characters?source=editorial&scope=all&q=" + q("museo-sombrero-9876")).body());
        assertThat(containsSlug(editorial, "h4-ui-1")).isTrue();
        JsonNode api = json.readTree(get("/api/v1/characters?source=api&scope=all&q=" + q("museo-sombrero-9876")).body());
        assertThat(containsSlug(api, "h4-ui-1")).isFalse();
        JsonNode namesOnly = json.readTree(get("/api/v1/characters?source=editorial&scope=name&q=" + q("museo-sombrero-9876")).body());
        assertThat(containsSlug(namesOnly, "h4-ui-1")).isFalse();
        JsonNode byName = json.readTree(get("/api/v1/characters?source=editorial&scope=name&q=" + q("H4 Investigador Unico")).body());
        assertThat(containsSlug(byName, "h4-ui-1")).isTrue();
    }

    @Test
    void episodeSeasonNavigationAndTitleScopeAreFromDatabase() throws Exception {
        jdbc.update("INSERT INTO episodes(slug,name,season,episode_number,airdate,synopsis) VALUES(?,?,?,?,CAST(? AS DATE),?)",
                "h4-catalogo-episodio-uno", "H4 Misterio A", 98, 989, "2020-01-01", "Un caso especial KZX963 en Springfield");
        jdbc.update("INSERT INTO episodes(slug,name,season,episode_number,airdate,synopsis) VALUES(?,?,?,?,CAST(? AS DATE),?)",
                "h4-catalogo-episodio-dos", "H4 Misterio B", 98, 990, "2020-01-02", "Una historia de archivo regional");
        JsonNode page = json.readTree(get("/api/v1/episodes?season=98&scope=title&order=chronological&size=20").body());
        assertThat(page.path("totalElements").asLong()).isEqualTo(2);
        assertThat(page.path("content").get(0).path("episodeNumber").asInt()).isEqualTo(989);
        assertThat(page.path("content").get(1).path("episodeNumber").asInt()).isEqualTo(990);
        JsonNode bySynopsis = json.readTree(get("/api/v1/episodes?season=98&scope=all&q=KZX963").body());
        assertThat(bySynopsis.path("totalElements").asLong()).isEqualTo(1);
        JsonNode byTitle = json.readTree(get("/api/v1/episodes?season=98&scope=title&q=KZX963").body());
        assertThat(byTitle.path("totalElements").asLong()).isZero();
        JsonNode seasons = json.readTree(get("/api/v1/episodes/seasons").body());
        boolean seasonPresent = false;
        for (JsonNode row : seasons) {
            if (row.path("season").asInt() == 98 && row.path("episodes").asLong() == 2) seasonPresent = true;
        }
        assertThat(seasonPresent).isTrue();
    }

    @Test
    void catalogRejectsUnknownQueryOptions() throws Exception {
        assertThat(get("/api/v1/characters?source=untrusted").status()).isEqualTo(400);
        assertThat(get("/api/v1/characters?scope=unknown").status()).isEqualTo(400);
        assertThat(get("/api/v1/characters?order=sql").status()).isEqualTo(400);
        assertThat(get("/api/v1/episodes?scope=occupation").status()).isEqualTo(400);
        assertThat(get("/api/v1/episodes?order=sql").status()).isEqualTo(400);
        assertThat(get("/api/v1/episodes?season=101").status()).isEqualTo(400);
    }

    @Test
    void consolidatedCatalogHidesVisualDuplicatesWithoutDeletingOriginalRecords() throws Exception {
        // Same proper name from the manual archive and from the external provider.
        charRow("h4-duplicate-manual", "H4 Persona Duplicada", "Archivo inicial", "Alive", "Anotación editorial");
        jdbc.update("INSERT INTO characters(slug,name,occupation,status,description,external_id,portrait_path) "
                + "VALUES(?,?,?,?,?,?,?)", "h4-duplicate-api", "H4 Persona Duplicada", "API externa",
                "Alive", "Nota de The Simpsons API", 980001, "/character/1.webp");
        JsonNode merged = json.readTree(get("/api/v1/characters?q=" + q("H4 Persona Duplicada")
                + "&source=curated&scope=name").body());
        assertThat(merged.path("totalElements").asInt()).isEqualTo(1);
        assertThat(merged.path("content").get(0).path("slug").asText()).isEqualTo("h4-duplicate-api");
        JsonNode raw = json.readTree(get("/api/v1/characters?q=" + q("H4 Persona Duplicada")
                + "&source=all&scope=name").body());
        assertThat(raw.path("totalElements").asInt()).isEqualTo(2);
        assertThat(get("/api/v1/characters/h4-duplicate-manual").status()).isEqualTo(200);
        assertThat(get("/api/v1/characters/h4-duplicate-api").status()).isEqualTo(200);

        locRow("h4-location-manual", "H4 Lugar Duplicado", "landmark", "Texto editorial");
        jdbc.update("INSERT INTO locations(slug,name,type,description,external_id,image_path) "
                + "VALUES(?,?,?,?,?,?)", "h4-location-api", "H4 Lugar Duplicado", "business",
                "Texto API", 980002, "/location/1.webp");
        JsonNode unified = json.readTree(get("/api/v1/locations?q=" + q("H4 Lugar Duplicado")
                + "&source=curated&scope=name").body());
        assertThat(unified.path("totalElements").asInt()).isEqualTo(1);
        assertThat(unified.path("content").get(0).path("slug").asText()).isEqualTo("h4-location-api");
        JsonNode variants = json.readTree(get("/api/v1/locations?q=" + q("H4 Lugar Duplicado")
                + "&source=all&scope=name").body());
        assertThat(variants.path("totalElements").asInt()).isEqualTo(2);
        JsonNode businessOnly = json.readTree(get("/api/v1/locations?q=" + q("H4 Lugar Duplicado")
                + "&type=business").body());
        assertThat(businessOnly.path("totalElements").asInt()).isEqualTo(1);
        assertThat(get("/api/v1/locations/h4-location-manual").status()).isEqualTo(200);
        assertThat(get("/api/v1/locations?type=broken").status()).isEqualTo(400);
        assertThat(get("/api/v1/locations?source=broken").status()).isEqualTo(400);
    }

    // ---------- Fichas individuales ----------

    @Test
    void characterDetailReturnsAllFieldsWithNullsAndIsoDate() throws Exception {
        charRow("h4-char-detail", "H4 Detalle", null, "Alive", "Desc detalle");
        HttpResult r = get("/api/v1/characters/h4-char-detail");
        assertThat(r.status()).isEqualTo(200);
        JsonNode body = json.readTree(r.body());
        assertThat(body.get("id").isNumber()).isTrue();
        assertThat(body.get("slug").asText()).isEqualTo("h4-char-detail");
        assertThat(body.get("name").asText()).isEqualTo("H4 Detalle");
        assertThat(body.get("occupation").isNull()).isTrue();
        assertThat(body.get("status").asText()).isEqualTo("Alive");
        assertThat(body.get("description").asText()).isEqualTo("Desc detalle");
        assertThat(body.get("createdAt").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[.+Z-].*");
    }

    @Test
    void locationDetailReturnsAllFields() throws Exception {
        locRow("h4-loc-detail", "H4 Lugar", "school", "Desc lugar");
        HttpResult r = get("/api/v1/locations/h4-loc-detail");
        assertThat(r.status()).isEqualTo(200);
        JsonNode body = json.readTree(r.body());
        assertThat(body.get("id").isNumber()).isTrue();
        assertThat(body.get("slug").asText()).isEqualTo("h4-loc-detail");
        assertThat(body.get("type").asText()).isEqualTo("school");
        assertThat(body.get("createdAt").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[.+Z-].*");
    }

    @Test
    void missingSlugsReturnStructured404() throws Exception {
        HttpResult char404 = get("/api/v1/characters/h4-no-existe");
        assertThat(char404.status()).isEqualTo(404);
        JsonNode charBody = json.readTree(char404.body());
        assertThat(charBody.get("status").asInt()).isEqualTo(404);
        assertThat(charBody.get("title").asText()).isNotBlank();

        HttpResult loc404 = get("/api/v1/locations/h4-no-existe");
        assertThat(loc404.status()).isEqualTo(404);
        JsonNode locBody = json.readTree(loc404.body());
        assertThat(locBody.get("status").asInt()).isEqualTo(404);
        assertThat(locBody.get("title").asText()).isNotBlank();
    }

    // ---------- Búsqueda de lugares ----------

    @Test
    void locationNameSearchIsPartialAndCaseInsensitive() throws Exception {
        locRow("h4-ln1", "H4 Taberna del Roble", "business", "Desc taberna");
        locRow("h4-ln2", "H4 Escuela Primaria", "school", "Desc escuela");
        JsonNode byTavern = json.readTree(get("/api/v1/locations?source=editorial&q=" + q("taberna")).body());
        assertThat(indexOfSlug(byTavern, "h4-ln1")).isGreaterThanOrEqualTo(0);
        assertThat(indexOfSlug(byTavern, "h4-ln2")).isEqualTo(-1);
        JsonNode byUpper = json.readTree(get("/api/v1/locations?source=editorial&q=" + q("TABERN")).body());
        assertThat(indexOfSlug(byUpper, "h4-ln1")).isGreaterThanOrEqualTo(0);
    }

    @Test
    void locationTypeSearchIsPartialAndCaseInsensitive() throws Exception {
        locRow("h4-lt1", "H4 Pradera del Roble", "park", "Desc parque");
        locRow("h4-lt2", "H4 Taller del Roble", "business", "Desc taller");
        JsonNode parks = json.readTree(get("/api/v1/locations?source=editorial&q=" + q("par")).body());
        assertThat(indexOfSlug(parks, "h4-lt1")).isGreaterThanOrEqualTo(0);
        assertThat(indexOfSlug(parks, "h4-lt2")).isEqualTo(-1);
        JsonNode upperBoth = json.readTree(get("/api/v1/locations?source=editorial&q=" + q("PAR")).body());
        assertThat(indexOfSlug(upperBoth, "h4-lt1")).isGreaterThanOrEqualTo(0);
        JsonNode businesses = json.readTree(get("/api/v1/locations?source=editorial&q=" + q("BUSINESS")).body());
        assertThat(indexOfSlug(businesses, "h4-lt2")).isGreaterThanOrEqualTo(0);
    }

    @Test
    void locationDescriptionSearchIsPartialAndCaseInsensitive() throws Exception {
        locRow("h4-ld1", "H4 Centro 1", "landmark", "Lugar favorito del SASTRE");
        locRow("h4-ld2", "H4 Centro 2", "landmark", "Otro sitio sin relacion");
        JsonNode matcher = json.readTree(get("/api/v1/locations?source=editorial&q=" + q("sastre")).body());
        assertThat(indexOfSlug(matcher, "h4-ld1")).isGreaterThanOrEqualTo(0);
        assertThat(indexOfSlug(matcher, "h4-ld2")).isEqualTo(-1);
        JsonNode upper = json.readTree(get("/api/v1/locations?source=editorial&q=" + q("SASTRE")).body());
        assertThat(indexOfSlug(upper, "h4-ld1")).isGreaterThanOrEqualTo(0);
    }

    @Test
    void locationSearchPercentIsLiteral() throws Exception {
        locRow("h4-lp", "H4 Cien % Completo", "landmark", "Desc pct");
        locRow("h4-lx", "H4 Cien x Completo", "landmark", "Desc x");
        JsonNode pct = json.readTree(get("/api/v1/locations?source=editorial&q=" + q("%")).body());
        assertThat(indexOfSlug(pct, "h4-lp")).isGreaterThanOrEqualTo(0);
        assertThat(indexOfSlug(pct, "h4-lx")).isEqualTo(-1);
    }

    @Test
    void locationSearchUnderscoreIsLiteral() throws Exception {
        locRow("h4-lu", "H4 A_B Molino", "landmark", "Desc und");
        locRow("h4-lv", "H4 AxB Molino", "landmark", "Desc ab");
        JsonNode und = json.readTree(get("/api/v1/locations?source=editorial&q=" + q("_")).body());
        assertThat(indexOfSlug(und, "h4-lu")).isGreaterThanOrEqualTo(0);
        assertThat(indexOfSlug(und, "h4-lv")).isEqualTo(-1);
    }

    @Test
    void locationSearchBackslashIsLiteral() throws Exception {
        locRow("h4-lbk", "H4 Barra \\ Literal", "landmark", "Desc back");
        locRow("h4-lother", "H4 Barra normal", "landmark", "Desc other");
        JsonNode back = json.readTree(get("/api/v1/locations?source=editorial&q=" + q("\\")).body());
        assertThat(indexOfSlug(back, "h4-lbk")).isGreaterThanOrEqualTo(0);
        assertThat(indexOfSlug(back, "h4-lother")).isEqualTo(-1);
    }

    @Test
    void locationSearchWithoutMatchesReturnsEmpty() throws Exception {
        locRow("h4-lnone", "H4 Sin Coincidencia", "landmark", "Desc none");
        JsonNode none = json.readTree(get("/api/v1/locations?source=editorial&q=" + q("zzz-sin-coincidencia")).body());
        assertThat(none.get("totalElements").asLong()).isZero();
        assertThat(none.get("content")).isEmpty();
    }

    // ---------- Contratos OpenAPI ----------

    @Test
    void openApiDocumentsPageResponseSchemasWithConcreteContentTypes() throws Exception {
        HttpResult apiDocs = get("/v3/api-docs");
        assertThat(apiDocs.status()).isEqualTo(200);
        JsonNode root = json.readTree(apiDocs.body());

        JsonNode schemas = root.path("components").path("schemas");

        // --- Characters ---
        JsonNode charsJson = root.path("paths").path("/api/v1/characters").path("get")
                .path("responses").path("200").path("content").path("*/*").path("schema");
        String charsRef = charsJson.path("$ref").asText("");
        assertThat(charsRef).endsWith("/schemas/PageResponseCharacterDto");

        JsonNode charPage = schemas.path("PageResponseCharacterDto");
        assertThat(charPage.isMissingNode()).isFalse();
        JsonNode charContent = charPage.path("properties").path("content");
        assertThat(charContent.path("type").asText()).isEqualTo("array");
        assertThat(charContent.path("items").path("$ref").asText("")).endsWith("/schemas/CharacterDto");
        assertThat(charPage.path("properties").path("page").path("type").asText()).isEqualTo("integer");
        assertThat(charPage.path("properties").path("size").path("type").asText()).isEqualTo("integer");
        assertThat(charPage.path("properties").path("totalElements").path("type").asText()).isEqualTo("integer");
        assertThat(charPage.path("properties").path("totalPages").path("type").asText()).isEqualTo("integer");
        assertThat(charPage.path("properties").path("first").path("type").asText()).isEqualTo("boolean");
        assertThat(charPage.path("properties").path("last").path("type").asText()).isEqualTo("boolean");

        // --- Locations ---
        JsonNode locsJson = root.path("paths").path("/api/v1/locations").path("get")
                .path("responses").path("200").path("content").path("*/*").path("schema");
        String locsRef = locsJson.path("$ref").asText("");
        assertThat(locsRef).endsWith("/schemas/PageResponseLocationDto");

        JsonNode locPage = schemas.path("PageResponseLocationDto");
        assertThat(locPage.isMissingNode()).isFalse();
        JsonNode locContent = locPage.path("properties").path("content");
        assertThat(locContent.path("type").asText()).isEqualTo("array");
        assertThat(locContent.path("items").path("$ref").asText("")).endsWith("/schemas/LocationDto");
        assertThat(locPage.path("properties").path("page").path("type").asText()).isEqualTo("integer");
        assertThat(locPage.path("properties").path("size").path("type").asText()).isEqualTo("integer");
        assertThat(locPage.path("properties").path("totalElements").path("type").asText()).isEqualTo("integer");
        assertThat(locPage.path("properties").path("totalPages").path("type").asText()).isEqualTo("integer");
    }

    private static boolean containsSlug(JsonNode page, String slug) {
        for (JsonNode item : page.get("content")) {
            if (slug.equals(item.get("slug").asText())) {
                return true;
            }
        }
        return false;
    }

    private static int indexOfSlug(JsonNode page, String slug) {
        for (int i = 0; i < page.get("content").size(); i++) {
            if (slug.equals(page.get("content").get(i).get("slug").asText())) {
                return i;
            }
        }
        return -1;
    }
}
