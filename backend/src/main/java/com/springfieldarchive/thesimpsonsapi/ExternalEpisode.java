package com.springfieldarchive.thesimpsonsapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Objeto episodio/short en The Simpsons API (misma estructura).
 * Los campos de fecha son ISO-8601; airdate puede venir null si el dato del proveedor falta.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalEpisode(
        @JsonProperty("id") long id,
        @JsonProperty("airdate") String airdate,
        @JsonProperty("episode_number") int episodeNumber,
        @JsonProperty("image_path") String imagePath,
        @JsonProperty("name") String name,
        @JsonProperty("season") int season,
        @JsonProperty("synopsis") String synopsis,
        @JsonProperty("description") String description) {
}
