package com.springfieldarchive.thesimpsonsapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Personaje tal como lo devuelve The Simpsons API (listado y detalle).
 * El detalle añade description, identificadores y objetos anidados de primera aparición
 * (null o ausentes cuando el proveedor no los trae).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalCharacter(
        @JsonProperty("id") long id,
        @JsonProperty("age") Integer age,
        @JsonProperty("birthdate") String birthdate,
        @JsonProperty("description") String description,
        @JsonProperty("first_appearance_ep_id") Long firstAppearanceEpId,
        @JsonProperty("first_appearance_sh_id") Long firstAppearanceShId,
        @JsonProperty("first_appearance_ep") ExternalEpisode firstAppearanceEp,
        @JsonProperty("first_appearance_sh") ExternalEpisode firstAppearanceSh,
        @JsonProperty("gender") String gender,
        @JsonProperty("name") String name,
        @JsonProperty("occupation") String occupation,
        @JsonProperty("phrases") List<String> phrases,
        @JsonProperty("portrait_path") String portraitPath,
        @JsonProperty("status") String status) {
}
