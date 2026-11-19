package com.springfieldarchive.thesimpsonsapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lugar tal como lo devuelve The Simpsons API.
 * El detalle añade description, identificadores y objetos anidados de primera aparición.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalLocation(
        @JsonProperty("id") long id,
        @JsonProperty("description") String description,
        @JsonProperty("first_appearance_ep_id") Long firstAppearanceEpId,
        @JsonProperty("first_appearance_sh_id") Long firstAppearanceShId,
        @JsonProperty("first_appearance_ep") ExternalEpisode firstAppearanceEp,
        @JsonProperty("first_appearance_sh") ExternalEpisode firstAppearanceSh,
        @JsonProperty("image_path") String imagePath,
        @JsonProperty("name") String name,
        @JsonProperty("town") String town,
        @JsonProperty("use") String use) {
}
