package com.springfieldarchive.sync;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AdminTokenVerifierTest {
    private static final String TOKEN = "a".repeat(48);

    @Test
    void productionRequiresMinimumLength() {
        assertThat(AdminTokenVerifier.valid(TOKEN, TOKEN, true)).isTrue();
        assertThat(AdminTokenVerifier.valid("weak", "weak", true)).isFalse();
        assertThat(AdminTokenVerifier.valid("weak", "weak", false)).isTrue();
    }

    @Test
    void rejectsMissingAndDifferentTokens() {
        assertThat(AdminTokenVerifier.valid(TOKEN, null, true)).isFalse();
        assertThat(AdminTokenVerifier.valid(null, TOKEN, true)).isFalse();
        assertThat(AdminTokenVerifier.valid(TOKEN, TOKEN + "x", true)).isFalse();
        assertThat(AdminTokenVerifier.valid(" ", " ", false)).isFalse();
    }
}
