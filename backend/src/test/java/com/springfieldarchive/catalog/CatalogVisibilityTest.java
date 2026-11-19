package com.springfieldarchive.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogVisibilityTest {
    @Test void curatedPrefersImportedRecordsInsteadOfMixingBothSources() {
        String sql = CatalogVisibility.from("curated", "characters", "id,name,external_id,portrait_path", "portrait_path");
        assertTrue(sql.contains("WHERE external_id IS NOT NULL"));
        assertFalse(sql.contains("NOT EXISTS"));
        assertTrue(sql.contains("ROW_NUMBER() OVER"));
        assertTrue(sql.contains("regexp_replace(btrim(name)"));
        assertTrue(sql.contains("external_id ASC NULLS LAST"));
    }
    @Test void editorialAndRawArchiveRemainAvailableWithoutDestructiveMerge() {
        assertEquals("characters", CatalogVisibility.from("all", "characters", "id", "portrait_path"));
        assertEquals("locations", CatalogVisibility.from("editorial", "locations", "id", "image_path"));
        String sql=CatalogVisibility.from("api", "locations", "id,name,external_id,image_path", "image_path");
        assertTrue(sql.contains("WHERE external_id IS NOT NULL"));
        assertFalse(sql.contains("NOT EXISTS"));
    }
    @Test void rejectsUnknownTablesAndImageColumns() {
        assertThrows(IllegalArgumentException.class,
            () -> CatalogVisibility.from("curated", "user_input", "id", "portrait_path"));
        assertThrows(IllegalArgumentException.class,
            () -> CatalogVisibility.from("curated", "characters", "id", "malicious_col"));
    }
}
