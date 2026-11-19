package com.springfieldarchive.catalog;

import com.springfieldarchive.dto.EpisodeDto;
import com.springfieldarchive.dto.EpisodePageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/episodes")
@Tag(name = "Episodios", description = "Catálogo público de solo lectura de episodios.")
public class EpisodeController {

    private final EpisodeRepository repository;

    public EpisodeController(EpisodeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Lista episodios", description = """
            Devuelve episodios de forma paginada. Orden determinista: temporada ASC, número global ASC, id ASC.
            El parámetro q busca en name, synopsis, description y slug de forma literal,
            sin distinguir mayúsculas. %, _ y \\ se interpretan literalmente.
            """)
    @ApiResponse(responseCode = "200", description = "Página de episodios (esquema PageResponse de EpisodeDto)",
            content = @Content(schema = @Schema(implementation = EpisodePageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Parámetros inválidos",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public EpisodePageResponse list(
            @Parameter(description = "Número de página (0..100). Por defecto 0.", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página (1..50). Por defecto 20.", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Texto de búsqueda literal (opcional)", example = "roasting")
            @RequestParam(required = false) String q,
            @Parameter(description = "Filtrar por temporada (opcional)", example = "1")
            @RequestParam(required = false) Integer season,
            @Parameter(description = "Buscar por titulo o en todos los campos", example = "title")
            @RequestParam(defaultValue = "all") String scope,
            @Parameter(description = "Orden: chronological, recent o title", example = "chronological")
            @RequestParam(defaultValue = "chronological") String order) {
        CatalogQueryValidator.requireValid(page, size, q);
        CatalogQueryValidator.requireSeason(season);
        CatalogQueryValidator.requireChoice("scope", scope, "all", "title");
        CatalogQueryValidator.requireChoice("order", order, "chronological", "recent", "title");
        return EpisodePageResponse.from(repository.findPage(q, season, null, scope, order, page, size));
    }

    @GetMapping("/seasons")
    @Operation(summary = "Lista temporadas disponibles con conteos reales de PostgreSQL")
    public java.util.List<EpisodeRepository.SeasonSummary> seasons() {
        return repository.findSeasons();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Ficha de episodio", description = "Devuelve el episodio con el slug indicado o 404 si no existe.")
    @ApiResponse(responseCode = "200", description = "Episodio encontrado",
            content = @Content(schema = @Schema(implementation = EpisodeDto.class)))
    @ApiResponse(responseCode = "404", description = "Episodio no encontrado",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public EpisodeDto bySlug(
            @Parameter(description = "Slug del episodio (p. ej. s1e1-simpsons-roasting-on-an-open-fire)", example = "s1e1-simpsons-roasting-on-an-open-fire")
            @PathVariable String slug) {
        return repository.findBySlug(slug).orElseThrow(() -> new NotFoundException("episodio"));
    }

    @GetMapping("/external/{externalId}")
    @Operation(summary = "Ficha de episodio por identificador externo", description = "Devuelve el episodio o 404 si no existe.")
    @ApiResponse(responseCode = "200", description = "Episodio encontrado",
            content = @Content(schema = @Schema(implementation = EpisodeDto.class)))
    @ApiResponse(responseCode = "404", description = "Episodio no encontrado",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public EpisodeDto byExternalId(
            @Parameter(description = "Identificador externo en The Simpsons API", example = "1")
            @PathVariable long externalId) {
        return repository.findByExternalId(externalId)
                .orElseThrow(() -> new NotFoundException("episodio"));
    }
}
