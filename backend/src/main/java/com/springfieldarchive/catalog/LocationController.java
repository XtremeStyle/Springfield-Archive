package com.springfieldarchive.catalog;

import com.springfieldarchive.dto.LocationDto;
import com.springfieldarchive.dto.LocationPageResponse;
import com.springfieldarchive.dto.PageResponse;
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
@RequestMapping("/api/v1/locations")
@Tag(name = "Lugares", description = "Catálogo público de solo lectura de lugares de Springfield.")
public class LocationController {

    private final LocationRepository repository;

    public LocationController(LocationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Lista lugares", description = """
            Devuelve lugares de forma paginada. Orden determinista: name ASC, id ASC.
            El parámetro q busca en name, type y description como texto literal, sin distinción de mayúsculas.
            Los caracteres %, _ y \\ se interpretan literalmente. Un q vacío o compuesto solo de espacios
            se trata como ausencia de filtro.
            """)
    @ApiResponse(responseCode = "200", description = "Página de lugares (esquema PageResponse de LocationDto)",
            content = @Content(schema = @Schema(implementation = LocationPageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Parámetros inválidos",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public LocationPageResponse list(
            @Parameter(description = "Número de página (0..100). Por defecto 0.", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página (1..50). Por defecto 20.", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Texto de búsqueda literal (opcional)", example = "taberna")
            @RequestParam(required = false) String q,
            @Parameter(description = "Vista: curated (consolidada), api, editorial, all (sin consolidar)")
            @RequestParam(defaultValue = "curated") String source,
            @Parameter(description = "Buscar solo en nombre o en todos los campos")
            @RequestParam(defaultValue = "all") String scope,
            @Parameter(description = "Orden: relevance, az, za, recent")
            @RequestParam(defaultValue = "az") String order,
            @Parameter(description = "Tipo: all, home, business, landmark, school, park o plant")
            @RequestParam(defaultValue = "all") String type) {
        CatalogQueryValidator.requireValid(page, size, q);
        CatalogQueryValidator.requireChoice("source", source, "curated", "api", "editorial", "all");
        CatalogQueryValidator.requireChoice("scope", scope, "all", "name");
        CatalogQueryValidator.requireChoice("order", order, "relevance", "az", "za", "recent");
        CatalogQueryValidator.requireChoice("type", type, "all", "home", "business", "landmark", "school", "park", "plant");
        return LocationPageResponse.from(repository.findPage(q, source, scope, order, type, page, size));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Ficha de lugar", description = "Devuelve el lugar con el slug indicado o 404 si no existe.")
    @ApiResponse(responseCode = "200", description = "Lugar encontrado",
            content = @Content(schema = @Schema(implementation = LocationDto.class)))
    @ApiResponse(responseCode = "404", description = "Lugar no encontrado",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public LocationDto bySlug(
            @Parameter(description = "Slug del lugar (p. ej. moe-s-tavern)", example = "moe-s-tavern")
            @PathVariable String slug) {
        return repository.findBySlug(slug).orElseThrow(() -> new NotFoundException("lugar"));
    }

    @GetMapping("/external/{externalId}")
    @Operation(summary = "Ficha de lugar por identificador externo", description = "Devuelve el lugar o 404 si no existe.")
    @ApiResponse(responseCode = "200", description = "Lugar encontrado",
            content = @Content(schema = @Schema(implementation = LocationDto.class)))
    @ApiResponse(responseCode = "404", description = "Lugar no encontrado",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public LocationDto byExternalId(
            @Parameter(description = "Identificador externo en The Simpsons API", example = "1")
            @PathVariable long externalId) {
        return repository.findByExternalId(externalId)
                .orElseThrow(() -> new NotFoundException("lugar"));
    }
}
