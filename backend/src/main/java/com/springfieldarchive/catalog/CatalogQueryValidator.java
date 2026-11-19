package com.springfieldarchive.catalog;

/** Límites uniformes para evitar consultas innecesariamente costosas. */
public final class CatalogQueryValidator {
    private static final int MAX_PAGE = 100;
    private static final int MAX_SIZE = 50;
    private static final int MAX_QUERY_LENGTH = 120;

    private CatalogQueryValidator() { }

    public static void requireValid(int page, int size, String query) {
        if (page < 0 || page > MAX_PAGE) {
            throw new IllegalArgumentException("page debe estar entre 0 y " + MAX_PAGE);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size debe estar entre 1 y " + MAX_SIZE);
        }
        if (query != null && query.strip().length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("q no puede tener más de " + MAX_QUERY_LENGTH + " caracteres");
        }
    }

    public static void requireSeason(Integer season) {
        if (season != null && (season < 0 || season > 100)) {
            throw new IllegalArgumentException("season debe estar entre 0 y 100");
        }
    }
    /** Parametros de ordenacion cerrados: nunca se interpolan valores arbitrarios en SQL. */
    public static void requireChoice(String key, String value, String... allowed) {
        if (value == null || java.util.Arrays.stream(allowed).noneMatch(value::equals)) {
            throw new IllegalArgumentException(key + " no es una opcion permitida");
        }
    }

}
