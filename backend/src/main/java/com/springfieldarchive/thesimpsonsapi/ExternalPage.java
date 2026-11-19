package com.springfieldarchive.thesimpsonsapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Respuesta de listados de The Simpsons API.
 * count/pages reales del proveedor; next/prev son URLs absolutas o null.
 */
public record ExternalPage<T>(
        @JsonProperty("count") int count,
        @JsonProperty("pages") int pages,
        @JsonProperty("next") String next,
        @JsonProperty("prev") String prev,
        @JsonProperty("results") List<T> results) {
}
