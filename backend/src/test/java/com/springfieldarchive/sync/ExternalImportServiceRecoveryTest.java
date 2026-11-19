package com.springfieldarchive.sync;

import com.springfieldarchive.thesimpsonsapi.ExternalApiException;
import com.springfieldarchive.thesimpsonsapi.ExternalCharacter;
import com.springfieldarchive.thesimpsonsapi.ExternalEpisode;
import com.springfieldarchive.thesimpsonsapi.ExternalLocation;
import com.springfieldarchive.thesimpsonsapi.ExternalPage;
import com.springfieldarchive.thesimpsonsapi.TheSimpsonsApiClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regresión: un registro individual fallido nunca puede cerrar una página
 * ni avanzar el checkpoint. No requiere Docker ni llamadas a Internet.
 */
class ExternalImportServiceRecoveryTest {

    private final TheSimpsonsApiClient client = mock(TheSimpsonsApiClient.class);
    private final CatalogSyncWriter writer = mock(CatalogSyncWriter.class);
    private final SyncStateRepository state = mock(SyncStateRepository.class);
    private final ExternalImportService service = new ExternalImportService(client, writer, state);

    @Test
    void aFailedCharacterDoesNotAdvanceThePageAndCanBeRetried() {
        ExternalCharacter first = new ExternalCharacter(101, null, null, null, null, null,
                null, null, null, "Homer Test", null, null, null, "Alive");
        ExternalCharacter second = new ExternalCharacter(102, null, null, null, null, null,
                null, null, null, "Marge Test", null, null, null, "Alive");
        when(state.find("characters")).thenReturn(Optional.empty());
        when(client.characters(1)).thenReturn(new ExternalPage<>(2, 1, null, null, List.of(first, second)));
        when(client.character(101)).thenReturn(Optional.of(first));
        when(client.character(102)).thenThrow(new ExternalApiException(503, "temporal"))
                .thenThrow(new ExternalApiException(503, "temporal"))
                .thenThrow(new ExternalApiException(503, "temporal"))
                .thenReturn(Optional.of(second));

        ExternalImportService.ResourceResult firstAttempt = service.runResource("characters");
        assertThat(firstAttempt.status()).isEqualTo("failed");
        assertThat(firstAttempt.processed()).isEqualTo(1);
        assertThat(firstAttempt.errors()).isEqualTo(1);
        verify(state).markFailed(eq("characters"), org.mockito.ArgumentMatchers.contains("102"));
        verify(state, never()).saveProgress(eq("characters"), anyInt());
        verify(state, never()).markCompleted(eq("characters"), anyInt(), anyInt());

        ExternalImportService.ResourceResult retried = service.runResource("characters");
        assertThat(retried.status()).isEqualTo("completed");
        assertThat(retried.processed()).isEqualTo(2);
        assertThat(retried.errors()).isZero();
        verify(state).markCompleted("characters", 2, 0);
    }

    @Test
    void aTransientDetailErrorIsRetriedBeforeFailingThePage() {
        ExternalCharacter item = new ExternalCharacter(401, null, null, null, null, null,
                null, null, null, "Lisa Simpson", null, null, null, "Alive");
        when(state.find("characters")).thenReturn(Optional.empty());
        when(client.characters(1)).thenReturn(new ExternalPage<>(1, 1, null, null, List.of(item)));
        when(client.character(401)).thenThrow(new ExternalApiException(503, "temporal"))
                .thenReturn(Optional.of(item));
        assertThat(service.runResource("characters").status()).isEqualTo("completed");
        verify(state).markCompleted("characters", 1, 0);
    }

    @Test
    void aMismatchedDetailIdNeverPollutesDatabase() {
        ExternalCharacter item = new ExternalCharacter(501, null, null, null, null, null,
                null, null, null, "Personaje", null, null, null, "Alive");
        ExternalCharacter wrong = new ExternalCharacter(502, null, null, null, null, null,
                null, null, null, "Otro personaje", null, null, null, "Alive");
        when(state.find("characters")).thenReturn(Optional.empty());
        when(client.characters(1)).thenReturn(new ExternalPage<>(1, 1, null, null, List.of(item)));
        when(client.character(501)).thenReturn(Optional.of(wrong));
        assertThat(service.runResource("characters").status()).isEqualTo("failed");
        verify(state, never()).saveProgress(eq("characters"), anyInt());
        verify(state, never()).markCompleted(eq("characters"), anyInt(), anyInt());
    }

    @Test
    void aFailedEpisodeDoesNotAdvanceThePage() {
        ExternalEpisode episode = new ExternalEpisode(201, "1990-01-01", 1,
                "/episode/201.webp", "Test Episode", 1, null, null);
        when(state.find("episodes")).thenReturn(Optional.empty());
        when(client.episodes(1)).thenReturn(new ExternalPage<>(1, 1, null, null, List.of(episode)));
        when(client.episode(201)).thenThrow(new ExternalApiException(502, "temporal"));

        assertThat(service.runResource("episodes").status()).isEqualTo("failed");
        verify(state, never()).saveProgress(eq("episodes"), anyInt());
        verify(state, never()).markCompleted(eq("episodes"), anyInt(), anyInt());
    }

    @Test
    void aFailedLocationDoesNotAdvanceThePage() {
        ExternalLocation location = new ExternalLocation(301, null, null, null,
                null, null, "/location/301.webp", "Test Location", "Springfield", "Shop");
        when(state.find("locations")).thenReturn(Optional.empty());
        when(client.locations(1)).thenReturn(new ExternalPage<>(1, 1, null, null, List.of(location)));
        when(client.location(301)).thenThrow(new ExternalApiException(502, "temporal"));

        assertThat(service.runResource("locations").status()).isEqualTo("failed");
        verify(state, never()).saveProgress(eq("locations"), anyInt());
        verify(state, never()).markCompleted(eq("locations"), anyInt(), anyInt());
    }
    @Test
    void aDuplicatedPageIdIsRejectedBeforeWritingAnyRecord() {
        ExternalCharacter first = new ExternalCharacter(701, null, null, null, null, null,
                null, null, null, "Personaje repetido", null, null, null, "Alive");
        when(state.find("characters")).thenReturn(Optional.empty());
        when(client.characters(1)).thenReturn(new ExternalPage<>(2, 1, null, null,
                List.of(first, first)));

        ExternalImportService.ResourceResult result = service.runResource("characters");
        assertThat(result.status()).isEqualTo("failed");
        verify(state, never()).saveProgress(eq("characters"), anyInt());
        verify(state, never()).markCompleted(eq("characters"), anyInt(), anyInt());
        verify(writer, never()).upsertCharacter(anyInt(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void aPageClaimingTooFewRecordsIsNotSilentlyCompleted() {
        ExternalLocation first = new ExternalLocation(801, null, null, null,
                null, null, null, "Lugar 1", "Springfield", "School");
        ExternalLocation second = new ExternalLocation(802, null, null, null,
                null, null, null, "Lugar 2", "Springfield", "Shop");
        when(state.find("locations")).thenReturn(Optional.empty());
        when(client.locations(1)).thenReturn(new ExternalPage<>(1, 1, null, null,
                List.of(first, second)));

        assertThat(service.runResource("locations").status()).isEqualTo("failed");
        verify(state, never()).markCompleted(eq("locations"), anyInt(), anyInt());
    }

    @Test
    void anUnexpectedFailureIsReportedInsteadOfLeavingSyncRunning() {
        // Fallo imprevisto fuera del cliente HTTP, al recuperar el checkpoint.
        // Un error HTTP normal lo transforma fetchWithRetry en resultado fallido.
        when(state.find("characters")).thenThrow(new IllegalStateException("fallo inesperado"));

        ExternalImportService.ResourceResult result = service.runResource("characters");
        assertThat(result.status()).isEqualTo("failed");
        verify(state).markFailed(eq("characters"), org.mockito.ArgumentMatchers.contains("Error inesperado"));
        verify(state, never()).markCompleted(eq("characters"), anyInt(), anyInt());
    }

}
