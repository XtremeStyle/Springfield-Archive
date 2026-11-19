package com.springfieldarchive.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO de paginación propio. No expone Page ni PageImpl de Spring Data.
 * Para un catálogo sin resultados: content=[], totalElements=0,
 * totalPages=0, first=true, last=true.
 */
@Schema(name = "PageResponse", description = "Respuesta paginada uniforme del catálogo")
public record PageResponse<T>(
        @Schema(description = "Elementos de la página, en el orden solicitado") List<T> content,
        @Schema(description = "Número de página (base 0; máximo 100)") int page,
        @Schema(description = "Tamaño de página (1..50)") int size,
        @Schema(description = "Total de elementos coincidentes") long totalElements,
        @Schema(description = "Número total de páginas (0 para catálogo vacío)") int totalPages,
        @Schema(description = "Indica si la página devuelta es la primera (page == 0)") boolean first,
        @Schema(description = "Indica si la página devuelta es la última (page >= totalPages - 1)") boolean last) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean last = page >= Math.max(totalPages - 1L, 0L);
        return new PageResponse<>(content, page, size, totalElements, totalPages, page == 0, last);
    }
}
