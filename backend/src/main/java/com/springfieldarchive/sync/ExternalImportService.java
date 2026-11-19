package com.springfieldarchive.sync;

import com.springfieldarchive.thesimpsonsapi.ExternalCharacter;
import com.springfieldarchive.thesimpsonsapi.ExternalEpisode;
import com.springfieldarchive.thesimpsonsapi.ExternalLocation;
import com.springfieldarchive.thesimpsonsapi.ExternalPage;
import com.springfieldarchive.thesimpsonsapi.ExternalApiException;
import com.springfieldarchive.thesimpsonsapi.TheSimpsonsApiClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

/**
 * Importa personajes, episodios y lugares de The Simpsons API hacia PostgreSQL.
 * Idempotente (upsert por external_id), reanudable (checkpoint por página en sync_state),
 * con reintentos limitados y pausa cortés entre peticiones.
 */
@Service
public class ExternalImportService {

    /** Pausa entre peticiones a la API externa (milisegundos). */
    private static final long REQUEST_DELAY_MS = 120L;
    /** Reintentos por página antes de declarar el recurso como fallido. */
    private static final int PAGE_RETRIES = 3;

    private final TheSimpsonsApiClient client;
    private final CatalogSyncWriter writer;
    private final SyncStateRepository stateRepository;

    public ExternalImportService(TheSimpsonsApiClient client,
                                 CatalogSyncWriter writer,
                                 SyncStateRepository stateRepository) {
        this.client = client;
        this.writer = writer;
        this.stateRepository = stateRepository;
    }

    /**
     * Ejecuta la importación completa de los tres recursos desde el principio o reanuda.
     */
    public SyncResult runAll() {
        return new SyncResult(runResource("characters"), runResource("episodes"), runResource("locations"));
    }

    public ResourceResult runResource(String resource) {
        if (!List.of("characters", "episodes", "locations").contains(resource)) {
            return new ResourceResult(resource, 0, 0, 0, "failed", "recurso desconocido");
        }
        try {
            int firstPage = resumeFrom(resource);
            return switch (resource) {
                case "characters" -> importCharacters(firstPage);
                case "episodes" -> importEpisodes(firstPage);
                case "locations" -> importLocations(firstPage);
                default -> new ResourceResult(resource, 0, 0, 0, "failed", "recurso desconocido");
            };
        } catch (RuntimeException error) {
            // Una excepción no prevista (p. ej. BD no disponible al guardar el progreso)
            // jamás debe dejar un estado "running" indefinido si aún podemos actualizarlo.
            String message = "Error inesperado al sincronizar " + resource
                    + " (" + error.getClass().getSimpleName() + ")";
            try {
                stateRepository.markFailed(resource, message);
            } catch (RuntimeException ignored) {
                // Puede que la misma caída de PostgreSQL impida registrar el fallo.
                // El recurso se comunica como fallido de todas formas; nunca como completo.
            }
            return new ResourceResult(resource, 0, 1, 0, "failed", message);
        }
    }

    private int resumeFrom(String resource) {
        return stateRepository.find(resource).map(SyncState::lastPage).orElse(0) + 1;
    }

    private ResourceResult importCharacters(int firstPage) {
        stateRepository.markRunning("characters");
        int processed = 0, errors = 0, pages = 0;
        int page = firstPage;
        while (true) {
            final int currentPage = page;
            ExternalPage<ExternalCharacter> result = fetchWithRetry(() -> client.characters(currentPage));
            if (result == null) {
                String message = "No se pudo consultar la página " + page + " de personajes";
                stateRepository.markFailed("characters", message);
                return new ResourceResult("characters", processed, errors + 1, pages, "failed", message);
            }
            if (!validPage(result, page)) {
                String message = "Paginación inconsistente para personajes en página " + page;
                stateRepository.markFailed("characters", message);
                return new ResourceResult("characters", processed, errors + 1, pages, "failed", message);
            }
            pages++;
            for (ExternalCharacter item : result.results()) {
                try {
                    validateExternal(item.id(), item.name());
                    Optional<ExternalCharacter> detail = fetchDetail(() -> client.character(item.id()));
                    if (detail.isPresent()) {
                        ExternalCharacter d = detail.get();
                        validateMatch(item.id(), d.id(), d.name());
                        writer.upsertCharacter((int) d.id(), d.name(), d.occupation(), d.status(),
                                d.description(), d.age(), d.birthdate(), d.gender(), d.phrases(),
                                d.portraitPath(), toInt(d.firstAppearanceEpId()), toInt(d.firstAppearanceShId()));
                    } else {
                        writer.upsertCharacter((int) item.id(), item.name(), item.occupation(), item.status(),
                                null, item.age(), item.birthdate(), item.gender(), item.phrases(),
                                item.portraitPath(), null, null);
                    }
                    processed++;
                } catch (RuntimeException e) {
                    // Nunca avanzar el checkpoint si un registro de esta página falló.
                    // En el siguiente intento se reprocesará la página completa de forma idempotente.
                    errors++;
                    String message = "No se pudo importar el ID externo " + item.id()
                            + " de personajes (página " + page + ")";
                    stateRepository.markFailed("characters", message);
                    return new ResourceResult("characters", processed, errors, pages, "failed", message);
                }
                pause();
            }
            if (page >= result.pages() || (result.count() == 0 && result.results().isEmpty())) {
                // Evita dejar un último checkpoint ya completado si el proceso se interrumpe.
                stateRepository.markCompleted("characters", processed, errors);
                return new ResourceResult("characters", processed, errors, pages, "completed", null);
            }
            stateRepository.saveProgress("characters", page);
            page++;
            pause();
        }
    }

    private ResourceResult importEpisodes(int firstPage) {
        stateRepository.markRunning("episodes");
        int processed = 0, errors = 0, pages = 0;
        int page = firstPage;
        while (true) {
            final int currentPage = page;
            ExternalPage<ExternalEpisode> result = fetchWithRetry(() -> client.episodes(currentPage));
            if (result == null) {
                String message = "No se pudo consultar la página " + page + " de episodios";
                stateRepository.markFailed("episodes", message);
                return new ResourceResult("episodes", processed, errors + 1, pages, "failed", message);
            }
            if (!validPage(result, page)) {
                String message = "Paginación inconsistente para episodios en página " + page;
                stateRepository.markFailed("episodes", message);
                return new ResourceResult("episodes", processed, errors + 1, pages, "failed", message);
            }
            pages++;
            for (ExternalEpisode item : result.results()) {
                try {
                    validateExternal(item.id(), item.name());
                    Optional<ExternalEpisode> detail = fetchDetail(() -> client.episode(item.id()));
                    ExternalEpisode d = detail.orElse(item);
                    validateMatch(item.id(), d.id(), d.name());
                    writer.upsertEpisode((int) d.id(), d.season(), d.episodeNumber(), d.airdate(),
                            d.name(), d.synopsis(), d.description(), d.imagePath());
                    processed++;
                } catch (RuntimeException e) {
                    // Nunca avanzar el checkpoint si un registro de esta página falló.
                    // En el siguiente intento se reprocesará la página completa de forma idempotente.
                    errors++;
                    String message = "No se pudo importar el ID externo " + item.id()
                            + " de episodios (página " + page + ")";
                    stateRepository.markFailed("episodes", message);
                    return new ResourceResult("episodes", processed, errors, pages, "failed", message);
                }
                pause();
            }
            if (page >= result.pages() || (result.count() == 0 && result.results().isEmpty())) {
                // Evita dejar un último checkpoint ya completado si el proceso se interrumpe.
                stateRepository.markCompleted("episodes", processed, errors);
                return new ResourceResult("episodes", processed, errors, pages, "completed", null);
            }
            stateRepository.saveProgress("episodes", page);
            page++;
            pause();
        }
    }

    private ResourceResult importLocations(int firstPage) {
        stateRepository.markRunning("locations");
        int processed = 0, errors = 0, pages = 0;
        int page = firstPage;
        while (true) {
            final int currentPage = page;
            ExternalPage<ExternalLocation> result = fetchWithRetry(() -> client.locations(currentPage));
            if (result == null) {
                String message = "No se pudo consultar la página " + page + " de lugares";
                stateRepository.markFailed("locations", message);
                return new ResourceResult("locations", processed, errors + 1, pages, "failed", message);
            }
            if (!validPage(result, page)) {
                String message = "Paginación inconsistente para lugares en página " + page;
                stateRepository.markFailed("locations", message);
                return new ResourceResult("locations", processed, errors + 1, pages, "failed", message);
            }
            pages++;
            for (ExternalLocation item : result.results()) {
                try {
                    validateExternal(item.id(), item.name());
                    Optional<ExternalLocation> detail = fetchDetail(() -> client.location(item.id()));
                    ExternalLocation d = detail.orElse(item);
                    validateMatch(item.id(), d.id(), d.name());
                    writer.upsertLocation((int) d.id(), d.name(), d.town(), d.use(),
                            d.description(), d.imagePath(), toInt(d.firstAppearanceEpId()), toInt(d.firstAppearanceShId()));
                    processed++;
                } catch (RuntimeException e) {
                    // Nunca avanzar el checkpoint si un registro de esta página falló.
                    // En el siguiente intento se reprocesará la página completa de forma idempotente.
                    errors++;
                    String message = "No se pudo importar el ID externo " + item.id()
                            + " de lugares (página " + page + ")";
                    stateRepository.markFailed("locations", message);
                    return new ResourceResult("locations", processed, errors, pages, "failed", message);
                }
                pause();
            }
            if (page >= result.pages() || (result.count() == 0 && result.results().isEmpty())) {
                // Evita dejar un último checkpoint ya completado si el proceso se interrumpe.
                stateRepository.markCompleted("locations", processed, errors);
                return new ResourceResult("locations", processed, errors, pages, "completed", null);
            }
            stateRepository.saveProgress("locations", page);
            page++;
            pause();
        }
    }

    /** No dar por cerrada una página vacía o retrocedida del proveedor. */
    private static boolean validPage(ExternalPage<?> result, int requestedPage) {
        if (result == null || result.results() == null || result.count() < 0 || result.pages() < 0) {
            return false;
        }
        if (result.count() == 0) {
            return requestedPage == 1 && result.results().isEmpty();
        }
        // Una página no puede declarar más registros que el total del proveedor,
        // contener un ID repetido o incluir una ficha nula. Evita checkpoints
        // que encubran un JSON truncado o una paginación corrupta.
        if (result.pages() < requestedPage || result.pages() > 10000
                || result.results().isEmpty() || result.results().size() > result.count()) {
            return false;
        }
        Set<Long> ids = new HashSet<>();
        for (Object item : result.results()) {
            long id;
            if (item instanceof ExternalCharacter character) { id = character.id(); }
            else if (item instanceof ExternalEpisode episode) { id = episode.id(); }
            else if (item instanceof ExternalLocation location) { id = location.id(); }
            else { return false; }
            if (id <= 0 || id > Integer.MAX_VALUE || !ids.add(id)) { return false; }
        }
        return true;
    }

    /** Nunca truncar identificadores externos ni aceptar fichas que rompen las constraints. */
    private static void validateExternal(long id, String name) {
        if (id <= 0 || id > Integer.MAX_VALUE || name == null || name.isBlank()
                || name.length() > 200) {
            throw new IllegalArgumentException("Identificador o nombre externo inválido");
        }
    }

    private static void validateMatch(long requestedId, long detailId, String detailName) {
        validateExternal(detailId, detailName);
        if (requestedId != detailId) {
            throw new IllegalArgumentException("La ficha externa no corresponde al ID solicitado");
        }
    }

    /** Un 404 confirmado permite recurrir a los datos de listado. Un error transitorio NO. */
    private <T> Optional<T> fetchDetail(java.util.function.Supplier<Optional<T>> call) {
        Optional<T> response = fetchWithRetry(call);
        if (response == null) {
            throw new ExternalApiException("No se pudo recuperar la ficha individual tras los reintentos");
        }
        return response;
    }

    private <T> T fetchWithRetry(java.util.function.Supplier<T> call) {
        long sleepMs = 500L;
        for (int attempt = 1; attempt <= PAGE_RETRIES; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException e) {
                if (!isRetryable(e) || attempt == PAGE_RETRIES) {
                    return null;
                }
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                sleepMs *= 2;
            }
        }
        return null;
    }

    private static boolean isRetryable(RuntimeException error) {
        if (!(error instanceof ExternalApiException api)) { return false; }
        int status = api.statusCode();
        // Fallos de red (sin HTTP) y respuestas transitoriamente saturadas.
        // JSON inválido y datos ausentes no mejoran por repetir la misma petición.
        return status == 429 || status == 408 || status >= 500
                || (status == -1 && api.getCause() instanceof java.io.IOException);
    }

    private void pause() {
        try {
            Thread.sleep(REQUEST_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Abortar en vez de continuar con el siguiente registro al cerrar Spring.
            // runResource marca "failed" y la siguiente ejecución reanuda la página.
            throw new IllegalStateException("Sincronización interrumpida");
        }
    }

    private static Integer toInt(Long value) {
        return value == null ? null : Math.toIntExact(value);
    }

    public record ResourceResult(String resource, int processed, int errors, int pages,
                                 String status, String message) {
    }

    public record SyncResult(ResourceResult characters, ResourceResult episodes, ResourceResult locations) {
    }
}
