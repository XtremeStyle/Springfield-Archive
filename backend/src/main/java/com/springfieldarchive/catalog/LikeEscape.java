package com.springfieldarchive.catalog;

/**
 * Escapa los comodines de LIKE/ILIKE para búsquedas literales:
 * backslash -> double backslash, después % y _. Usa '\' como escape.
 */
public final class LikeEscape {

    private LikeEscape() {
    }

    public static String escape(String literal) {
        return literal
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
