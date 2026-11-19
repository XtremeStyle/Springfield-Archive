package com.springfieldarchive.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PageResponseEpisodeDto", description = "Respuesta paginada de episodios (PageResponse de EpisodeDto)")
public record EpisodePageResponse(
        @Schema(description = "Elementos de la página, en el orden solicitado") List<EpisodeDto> content,
        @Schema(description = "Número de página (base 0; máximo 100)") int page,
        @Schema(description = "Tamaño de página (1..50)") int size,
        @Schema(description = "Total de elementos coincidentes") long totalElements,
        @Schema(description = "Número total de páginas (0 para catálogo vacío)") int totalPages,
        @Schema(description = "Indica si la página devuelta es la primera (page == 0)") boolean first,
        @Schema(description = "Indica si la página devuelta es la última (page >= totalPages - 1)") boolean last) {

    public static EpisodePageResponse from(PageResponse<EpisodeDto> source) {
        return new EpisodePageResponse(source.content(), source.page(), source.size(),
                source.totalElements(), source.totalPages(), source.first(), source.last());
    }
}
