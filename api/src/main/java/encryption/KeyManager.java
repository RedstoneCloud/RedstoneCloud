package encryption;

import com.google.common.base.Preconditions;
import exception.EncryptionException;
import lombok.Getter;
import util.B64;

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

    public static String encrypt(String message, PublicKey key) {
        Preconditions.checkNotNull(key, "Encryption keys not initialized");

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes = doFinal(messageBytes, Cipher.ENCRYPT_MODE, key);

        return B64.encode(encryptedBytes);
    }

    public static String decrypt(String message) {
        Preconditions.checkNotNull(privateKey, "Encryption keys not initialized");

        byte[] encryptedBytes = B64.decode(message);
        byte[] messageBytes = doFinal(encryptedBytes, Cipher.DECRYPT_MODE, privateKey);

        return new String(messageBytes, StandardCharsets.UTF_8);
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
