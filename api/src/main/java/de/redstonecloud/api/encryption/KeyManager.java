package de.redstonecloud.api.encryption;

import com.google.common.base.Preconditions;
import de.redstonecloud.api.exception.EncryptionException;
import lombok.Getter;
import de.redstonecloud.api.util.B64;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class KeyManager {
    @Getter private static PublicKey publicKey = null;
    private static PrivateKey privateKey = null;

    public static PublicKey init() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(4096);

            KeyPair pair = generator.generateKeyPair();

            publicKey = pair.getPublic();
            privateKey = pair.getPrivate();

            return publicKey;
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    public static byte[] encrypt(byte[] message, PublicKey key) {
        Preconditions.checkNotNull(key, "Encryption keys not initialized");

        return doFinal(message, Cipher.ENCRYPT_MODE, key);
    }

    public static byte[] decrypt(byte[] message) {
        Preconditions.checkNotNull(privateKey, "Encryption keys not initialized");

        return doFinal(message, Cipher.DECRYPT_MODE, privateKey);
    }

    private static byte[] doFinal(byte[] message, int mode, AsymmetricKey key) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(mode, key);

            return cipher.doFinal(message);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }
}
