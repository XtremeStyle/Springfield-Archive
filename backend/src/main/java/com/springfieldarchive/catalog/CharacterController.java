package com.springfieldarchive.catalog;

import com.springfieldarchive.dto.CharacterDto;
import com.springfieldarchive.dto.CharacterPageResponse;
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
@RequestMapping("/api/v1/characters")
@Tag(name = "Personajes", description = "Catálogo público de solo lectura de personajes de Springfield.")
public class CharacterController {

    private final CharacterRepository repository;

    public CharacterController(CharacterRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Lista personajes", description = """
            Devuelve personajes de forma paginada. Orden determinista: name ASC, id ASC.
            El parámetro q busca en name, occupation y description como texto literal, sin distinción de mayúsculas.
            Los caracteres %, _ y \\ se interpretan literalmente. Un q vacío o compuesto solo de espacios
            se trata como ausencia de filtro.
            """)
    @ApiResponse(responseCode = "200", description = "Página de personajes (esquema PageResponse de CharacterDto)",
            content = @Content(schema = @Schema(implementation = CharacterPageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Parámetros inválidos",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public CharacterPageResponse list(
            @Parameter(description = "Número de página (0..100). Por defecto 0.", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página (1..50). Por defecto 20.", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Texto de búsqueda literal (opcional)", example = "homero")
            @RequestParam(required = false) String q,
            @Parameter(description = "Procedencia: curated (sin duplicados), api, editorial o all (registros sin consolidar)", example = "api")
            @RequestParam(defaultValue = "curated") String source,
            @Parameter(description = "Buscar en nombre solamente o todos los campos", example = "name")
            @RequestParam(defaultValue = "all") String scope,
            @Parameter(description = "Orden relevance, az, za o recent", example = "az")
            @RequestParam(defaultValue = "az") String order) {
        CatalogQueryValidator.requireValid(page, size, q);
        CatalogQueryValidator.requireChoice("source", source, "curated", "all", "api", "editorial");
        CatalogQueryValidator.requireChoice("scope", scope, "all", "name");
        CatalogQueryValidator.requireChoice("order", order, "relevance", "az", "za", "recent");
        return CharacterPageResponse.from(repository.findPage(q, source, scope, order, page, size));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Ficha de personaje", description = "Devuelve el personaje con el slug indicado o 404 si no existe.")
    @ApiResponse(responseCode = "200", description = "Personaje encontrado",
            content = @Content(schema = @Schema(implementation = CharacterDto.class)))
    @ApiResponse(responseCode = "404", description = "Personaje no encontrado",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public CharacterDto bySlug(
            @Parameter(description = "Slug del personaje (p. ej. homer-simpson)", example = "homer-simpson")
            @PathVariable String slug) {
        return repository.findBySlug(slug).orElseThrow(() -> new NotFoundException("personaje"));
    }

    @GetMapping("/external/{externalId}")
    @Operation(summary = "Ficha de personaje por identificador externo", description = "Devuelve el personaje o 404 si no existe.")
    @ApiResponse(responseCode = "200", description = "Personaje encontrado",
            content = @Content(schema = @Schema(implementation = CharacterDto.class)))
    @ApiResponse(responseCode = "404", description = "Personaje no encontrado",
            content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    public CharacterDto byExternalId(
            @Parameter(description = "Identificador externo en The Simpsons API", example = "1")
            @PathVariable long externalId) {
        return repository.findByExternalId(externalId)
                .orElseThrow(() -> new NotFoundException("personaje"));
    }
}
