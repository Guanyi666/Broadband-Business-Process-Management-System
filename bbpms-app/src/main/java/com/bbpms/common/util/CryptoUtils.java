package com.bbpms.common.util;

import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Crypto helpers: SM4 (BC, CBC PKCS7), SM3 hash, RSA, masking.
 */
public final class CryptoUtils {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private CryptoUtils() {}

    /* -------------------- SM4 (CBC + PKCS7 via BouncyCastle) -------------------- */

    /**
     * Encrypt with explicit key and IV (hex).
     */
    public static String sm4Encrypt(String keyHex, String ivHex, String plaintext) {
        try {
            byte[] key = hexToBytes(keyHex);
            byte[] iv = hexToBytes(ivHex);
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new SM4Engine()));
            cipher.init(true, new ParametersWithIV(new KeyParameter(key), iv));
            byte[] in = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] out = new byte[cipher.getOutputSize(in.length)];
            int len = cipher.processBytes(in, 0, in.length, out, 0);
            len += cipher.doFinal(out, len);
            byte[] result = new byte[len];
            System.arraycopy(out, 0, result, 0, len);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("SM4 encrypt failed", e);
        }
    }

    /** Default IV (16 bytes all-zero, 32 hex chars). */
    private static final String DEFAULT_IV = "00000000000000000000000000000000";

    /** Default SM4 key derived from SM3 of a well-known string (first 32 hex chars → 16 bytes). */
    private static final String DEFAULT_SM4_KEY = sm3("bbpms-default-sm4-key").substring(0, 32);

    /** SM4 encrypt with default key + IV. */
    public static String sm4Encrypt(String plaintext) {
        return sm4Encrypt(DEFAULT_SM4_KEY, DEFAULT_IV, plaintext);
    }

    /** SM4 decrypt with default key + IV. */
    public static String sm4Decrypt(String cipherBase64) {
        return sm4Decrypt(DEFAULT_SM4_KEY, DEFAULT_IV, cipherBase64);
    }

    /**
     * Encrypt with key only (uses all-zero IV).
     */
    public static String sm4Encrypt(String plaintext, String keyHex) {
        return sm4Encrypt(keyHex, DEFAULT_IV, plaintext);
    }

    public static String sm4Decrypt(String keyHex, String ivHex, String cipherBase64) {
        try {
            byte[] key = hexToBytes(keyHex);
            byte[] iv = hexToBytes(ivHex);
            byte[] ct = Base64.getDecoder().decode(cipherBase64);
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new SM4Engine()));
            cipher.init(false, new ParametersWithIV(new KeyParameter(key), iv));
            byte[] out = new byte[cipher.getOutputSize(ct.length)];
            int len = cipher.processBytes(ct, 0, ct.length, out, 0);
            len += cipher.doFinal(out, len);
            return new String(out, 0, len, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            throw new IllegalStateException("SM4 decrypt failed", e);
        }
    }

    /**
     * Decrypt with key only (uses all-zero IV).
     */
    public static String sm4Decrypt(String cipherBase64, String keyHex) {
        return sm4Decrypt(keyHex, DEFAULT_IV, cipherBase64);
    }

    /* -------------------- SM3 -------------------- */
    public static String sm3(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SM3", BouncyCastleProvider.PROVIDER_NAME);
            byte[] h = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(h);
        } catch (Exception e) {
            throw new IllegalStateException("SM3 failed", e);
        }
    }

    /* -------------------- RSA -------------------- */
    public static KeyPair rsaGenerate(int bits) {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(bits);
            return g.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("RSA keygen failed", e);
        }
    }

    /** Convenience alias for rsaDecryptBase64. */
    public static String rsaDecrypt(String encryptedBase64, String privateKeyBase64) {
        return rsaDecryptBase64(privateKeyBase64, encryptedBase64);
    }

    public static String rsaEncryptBase64(String publicKeyBase64, String plaintext) {
        try {
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.ENCRYPT_MODE, loadPublicKey(publicKeyBase64));
            return Base64.getEncoder().encodeToString(c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("RSA encrypt failed", e);
        }
    }

    public static String rsaDecryptBase64(String privateKeyBase64, String cipherBase64) {
        try {
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.DECRYPT_MODE, loadPrivateKey(privateKeyBase64));
            return new String(c.doFinal(Base64.getDecoder().decode(cipherBase64)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("RSA decrypt failed", e);
        }
    }

    /* -------------------- key loading -------------------- */
    public static PublicKey loadPublicKey(String base64) {
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalArgumentException("bad public key", e);
        }
    }

    public static PrivateKey loadPrivateKey(String base64) {
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalArgumentException("bad private key", e);
        }
    }

    /* -------------------- masking -------------------- */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String maskIdCard(String id) {
        if (id == null || id.length() < 10) return id;
        return id.substring(0, 6) + "********" + id.substring(id.length() - 4);
    }

    public static String maskName(String name) {
        if (name == null || name.isBlank()) return name;
        String s = name.trim();
        if (s.length() == 1) return s;
        if (s.length() == 2) return s.charAt(0) + "*";
        // Keep first and last char, mask the middle (supports multi-char names).
        return s.charAt(0) + "*".repeat(s.length() - 2) + s.charAt(s.length() - 1);
    }

    /* -------------------- utils -------------------- */
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                | Character.digit(hex.charAt(i + 1), 16));
        return out;
    }

    private static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
