package com.springfieldarchive.sync;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Guarda contra ejecuciones concurrentes de la importación.
 * Un AtomicBoolean "en vuelo" y contador de peticiones realizadas.
 */
@Component
public class ImportGuard {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong startedAt = new AtomicLong(0L);

    public boolean tryStart() {
        if (running.compareAndSet(false, true)) {
            startedAt.set(System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public void finish() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public long runningSinceMillis() {
        return startedAt.get();
    }
}
