package com.springfieldarchive.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Esquema OpenAPI especializado: respuesta paginada de lugares.
 * Misma forma JSON que PageResponse pero con elementos tipados.
 */
@Schema(name = "PageResponseLocationDto", description = "Respuesta paginada de lugares (PageResponse de LocationDto)")
public record LocationPageResponse(
        @Schema(description = "Elementos de la página, en el orden solicitado") List<LocationDto> content,
        @Schema(description = "Número de página (base 0; máximo 100)") int page,
        @Schema(description = "Tamaño de página (1..50)") int size,
        @Schema(description = "Total de elementos coincidentes") long totalElements,
        @Schema(description = "Número total de páginas (0 para catálogo vacío)") int totalPages,
        @Schema(description = "Indica si la página devuelta es la primera (page == 0)") boolean first,
        @Schema(description = "Indica si la página devuelta es la última (page >= totalPages - 1)") boolean last) {

    public static LocationPageResponse from(PageResponse<LocationDto> source) {
        return new LocationPageResponse(source.content(), source.page(), source.size(),
                source.totalElements(), source.totalPages(), source.first(), source.last());
    }
}
