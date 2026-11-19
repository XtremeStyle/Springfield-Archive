package com.springfieldarchive.sync;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Verificación de token para una acción administrativa. No implementa gestión de usuarios. */
public final class AdminTokenVerifier {
    private AdminTokenVerifier() { }

    public static boolean valid(String configured, String supplied, boolean production) {
        if (configured == null || configured.isBlank() || supplied == null || supplied.isBlank()
                || configured.length() > 512 || supplied.length() > 512
                || (production && configured.length() < 32)) {
            return false;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] expectedHash = digest.digest(configured.getBytes(StandardCharsets.UTF_8));
            byte[] receivedHash = digest.digest(supplied.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(expectedHash, receivedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en este JDK", e);
        }
    }
}
