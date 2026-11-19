package com.springfieldarchive.sync;

import java.time.OffsetDateTime;

public record SyncState(
        String resource,
        String status,
        int lastPage,
        int processed,
        int errors,
        OffsetDateTime syncedAt,
        OffsetDateTime updatedAt,
        String error) {
}
