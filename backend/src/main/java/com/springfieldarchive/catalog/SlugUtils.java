package com.springfieldarchive.catalog;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Genera slugs deterministas desde un texto libre.
 */
public final class SlugUtils {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private SlugUtils() {
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "unknown";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String slug = NON_ALNUM.matcher(normalized).replaceAll("-")
                .replaceAll("-+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return slug.isEmpty() ? "unknown" : slug;
    }
}
