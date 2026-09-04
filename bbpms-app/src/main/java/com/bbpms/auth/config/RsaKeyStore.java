package com.bbpms.auth.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Persists the RSA keypair (and refresh secret) to a local file so that the
 * keypair survives application restarts.
 *
 * <p>Before this store existed, the keypair was regenerated on every start when
 * {@code bbpms.jwt.rsa-* } was empty (the default in dev), so the public key a
 * user's browser fetched on page load could belong to a previous process — the
 * backend then failed to decrypt the login password ("Password decryption
 * failed"). Persisting the keys makes the public key stable across restarts.
 */
public final class RsaKeyStore {

    /** Default location: {@code ~/.bbpms/rsa-keys.properties}. */
    private static final String DEFAULT_FILE = ".bbpms" + File.separator + "rsa-keys.properties";

    private RsaKeyStore() {
    }

    public static File keysFile() {
        String override = System.getenv("BBPMS_KEYS_FILE");
        if (override != null && !override.isBlank()) {
            return new File(override.trim());
        }
        String home = System.getProperty("user.home");
        return new File(home, DEFAULT_FILE);
    }

    /** Returns the persisted properties, or {@code null} if no readable file exists. */
    public static Properties load() {
        File f = keysFile();
        if (!f.isFile()) {
            return null;
        }
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(f)) {
            p.load(in);
            if (p.getProperty("rsaPrivateKey") == null || p.getProperty("rsaPublicKey") == null) {
                return null;
            }
            return p;
        } catch (IOException e) {
            return null;
        }
    }

    /** Atomically writes the keypair to disk (best-effort; failures are logged by caller). */
    public static void save(String privateKey, String publicKey, String refreshSecret) {
        File f = keysFile();
        File dir = f.getParentFile();
        if (dir != null && !dir.isDirectory()) {
            dir.mkdirs();
        }
        Properties p = new Properties();
        p.setProperty("rsaPrivateKey", privateKey);
        p.setProperty("rsaPublicKey", publicKey);
        if (refreshSecret != null) {
            p.setProperty("refreshSecret", refreshSecret);
        }
        File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            p.store(out, "BBPMS RSA keys. Keep private.");
            out.flush();
        } catch (IOException e) {
            try {
                tmp.delete();
            } catch (Exception ignored) {
                // nothing more we can do
            }
            throw new IllegalStateException("Failed to write RSA keys to " + tmp.getAbsolutePath(), e);
        }
        try {
            // REPLACE_EXISTING without ATOMIC_MOVE: atomic move on Windows
            // can fail when the destination already exists, so use the plain
            // replace which is reliable on all platforms.
            java.nio.file.Files.move(tmp.toPath(), f.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                tmp.delete();
            } catch (Exception ignored) {
                // nothing more we can do
            }
            throw new IllegalStateException("Failed to persist RSA keys to " + f.getAbsolutePath(), e);
        }
    }
}