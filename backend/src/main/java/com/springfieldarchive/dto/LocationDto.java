package com.springfieldarchive.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "LocationDto", description = "Lugar de Springfield del catálogo de Springfield Archive")
public record LocationDto(
        @Schema(description = "Identificador interno del lugar (BD local)") long id,
        @Schema(description = "Identificador externo en The Simpsons API; null para el catálogo manual") Integer externalId,
        @Schema(description = "Slug único del lugar (p. ej. moe-s-tavern)") String slug,
        @Schema(description = "Nombre del lugar") String name,
        @Schema(description = "Tipo de lugar",
                allowableValues = {"home", "business", "landmark", "school", "park", "plant"}) String type,
        @Schema(description = "Descripción editorial del lugar") String description,
        @Schema(description = "Localidad/town; null si no consta") String town,
        @Schema(description = "Uso declarado por el proveedor; null si no consta") String use,
        @Schema(description = "Ruta de la imagen en el CDN del proveedor; null si no hay imagen") String imagePath,
        @Schema(description = "Identificador externo del episodio de primera aparición, o null") Integer firstAppearanceEpId,
        @Schema(description = "Identificador externo del short de primera aparición, o null") Integer firstAppearanceShId,
        @Schema(description = "Fecha de creación del registro (ISO-8601)") OffsetDateTime createdAt) {
}
