package com.springfieldarchive.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(name = "CharacterDto", description = "Personaje del catálogo de Springfield Archive")
public record CharacterDto(
        @Schema(description = "Identificador interno del personaje (BD local)") long id,
        @Schema(description = "Identificador externo en The Simpsons API; null para el catálogo manual") Integer externalId,
        @Schema(description = "Slug único del personaje (p. ej. homer-simpson)") String slug,
        @Schema(description = "Nombre del personaje") String name,
        @Schema(description = "Edad publicada por The Simpsons API; null cuando no consta") Integer age,
        @Schema(description = "Ocupación o cometido; null cuando no consta en el catálogo") String occupation,
        @Schema(description = "Estado vital/narrativo del personaje", allowableValues = {"Alive", "Deceased", "Unknown"}) String status,
        @Schema(description = "Descripción editorial del personaje") String description,
        @Schema(description = "Fecha de nacimiento ISO-8601, o null si no consta") String birthdate,
        @Schema(description = "Género declarado; null si no consta") String gender,
        @Schema(description = "Frases características; null para el catálogo manual") List<String> phrases,
        @Schema(description = "Ruta de la imagen en el CDN del proveedor; null si no hay imagen") String portraitPath,
        @Schema(description = "Identificador externo del episodio de primera aparición, o null") Integer firstAppearanceEpId,
        @Schema(description = "Identificador externo del short de primera aparición, o null") Integer firstAppearanceShId,
        @Schema(description = "Fecha de creación del registro (ISO-8601)") OffsetDateTime createdAt) {
}
