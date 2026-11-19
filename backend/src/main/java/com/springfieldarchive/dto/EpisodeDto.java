package com.springfieldarchive.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "EpisodeDto", description = "Episodio de The Simpsons del catálogo de Springfield Archive")
public record EpisodeDto(
        @Schema(description = "Identificador interno del episodio (BD local)") long id,
        @Schema(description = "Identificador externo en The Simpsons API") Integer externalId,
        @Schema(description = "Slug único del episodio (p. ej. s1e1-simpsons-roasting-on-an-open-fire)") String slug,
        @Schema(description = "Temporada (1-40); null si no consta") Integer season,
        @Schema(description = "Número global de episodio en la serie, no ordinal dentro de la temporada; null si no consta") Integer episodeNumber,
        @Schema(description = "Fecha de emisión ISO-8601, o null si no consta") String airdate,
        @Schema(description = "Título del episodio") String name,
        @Schema(description = "Sinopsis del episodio") String synopsis,
        @Schema(description = "Descripción extensa; null si el proveedor no la aporta") String description,
        @Schema(description = "Ruta de la imagen en el CDN del proveedor; null si no hay imagen") String imagePath,
        @Schema(description = "Fecha de creación del registro (ISO-8601)") OffsetDateTime createdAt) {
}
