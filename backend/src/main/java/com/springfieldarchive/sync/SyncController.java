package com.springfieldarchive.sync;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

@RestController
@Tag(name = "Sincronización", description = "Importación controlada del catálogo desde The Simpsons API")
public class SyncController {

    // Una cola por instancia Spring, cerrada al detener la aplicación.
    // El despliegue gratuito previsto utiliza UNA réplica del backend.
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "springfield-archive-sync");
        t.setDaemon(true);
        return t;
    });

    private final Environment env;
    private final ImportGuard guard;
    private final ExternalImportService importer;
    private final SyncStateRepository stateRepository;

    public SyncController(Environment env,
                          ImportGuard guard,
                          ExternalImportService importer,
                          SyncStateRepository stateRepository) {
        this.env = env;
        this.guard = guard;
        this.importer = importer;
        this.stateRepository = stateRepository;
    }

    @PostMapping("/api/v1/sync/start")
    @Operation(summary = "Dispara la importación", description = """
            Lanza la importación desde TheSimpsons API hacia PostgreSQL local.
            Requiere el header X-Admin-Token idéntico al valor de la variable
            de entorno SPRINGFIELD_ADMIN_TOKEN (ver OPERATIONS.md).
            La importación es idempotente, reanudable y se ejecuta en segundo plano.
            """)
    @ApiResponse(responseCode = "202", description = "Importación encolada en segundo plano")
    @ApiResponse(responseCode = "403", description = "Token X-Admin-Token no configurado o inválido")
    @ApiResponse(responseCode = "409", description = "Ya hay una importación en ejecución")
    public ResponseEntity<Map<String, Object>> start(
            @Parameter(description = "Recurso a importar: all|characters|episodes|locations", example = "all")
            @RequestParam(defaultValue = "all") String resource,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        String expected = env.getProperty("springfield.admin.token");
        if (!AdminTokenVerifier.valid(expected, token, env.acceptsProfiles(Profiles.of("prod")))) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "forbidden",
                    "detail", "Acceso denegado"));
        }

        if (!List.of("all", "characters", "episodes", "locations").contains(resource)) {
            return ResponseEntity.badRequest().body(Map.of("error", "bad_request",
                    "detail", "resource debe ser all|characters|episodes|locations"));
        }

        if (!guard.tryStart()) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "busy",
                    "detail", "ya hay una importación en ejecución",
                    "sinceMillis", guard.runningSinceMillis()));
        }

        try {
            executor.execute(() -> {
                try {
                    switch (resource) {
                        case "all" -> importer.runAll();
                        case "characters", "episodes", "locations" -> importer.runResource(resource);
                    }
                } finally {
                    guard.finish();
                }
            });
        } catch (RejectedExecutionException e) {
            guard.finish();
            return ResponseEntity.status(503).body(Map.of(
                    "error", "unavailable", "detail", "el proceso de sincronización se está deteniendo"));
        }

        return ResponseEntity.accepted().body(Map.of(
                "status", "started",
                "resource", resource,
                "detail", "importación en segundo plano; consulta /api/v1/sync/status"));
    }

    @PreDestroy
    public void closeExecutor() {
        // Un proceso detenido no deja tareas en una cola estática de la JVM.
        executor.shutdownNow();
    }

}
