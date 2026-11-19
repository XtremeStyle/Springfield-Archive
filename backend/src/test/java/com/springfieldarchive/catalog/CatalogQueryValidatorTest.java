package com.springfieldarchive.catalog;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogQueryValidatorTest {
    @Test void acceptsLegalParameters() {
        assertThatCode(() -> CatalogQueryValidator.requireValid(0,20,"Homer"))
            .doesNotThrowAnyException();
        assertThatCode(() -> CatalogQueryValidator.requireValid(100,50,null))
            .doesNotThrowAnyException();
        assertThatCode(() -> CatalogQueryValidator.requireSeason(33))
            .doesNotThrowAnyException();
    }
    @Test void rejectsOversizedSearchAndUnboundedPage() {
        assertThatThrownBy(() -> CatalogQueryValidator.requireValid(0,20,"x".repeat(121)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CatalogQueryValidator.requireValid(101,20,null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CatalogQueryValidator.requireValid(0,51,null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CatalogQueryValidator.requireSeason(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
