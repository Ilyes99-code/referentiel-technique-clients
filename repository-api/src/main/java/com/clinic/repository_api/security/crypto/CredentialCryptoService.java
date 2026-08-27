package com.clinic.repository_api.security.crypto;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Encrypts/decrypts stored secrets (technical-access passwords) with AES-256-GCM.
 *
 * This protects data at rest — a DB dump, backup, or ad-hoc SQL query never sees
 * plaintext. It intentionally does NOT hide the value from the application layer:
 * the API is expected to keep returning the decrypted password to authenticated
 * admins (product requirement for the credentials vault feature).
 */
@Service
public class CredentialCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialCryptoService(@Value("${app.crypto.secret}") String base64Secret) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "app.crypto.secret must decode to a 32-byte (256-bit) key, got " + keyBytes.length + " bytes");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt credential value", e);
        }
    }

    public String decrypt(String storedValue) {
        if (storedValue == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(storedValue);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to decrypt credential value — wrong key or corrupted data", e);
        }
    }
}
