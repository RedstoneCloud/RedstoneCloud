package encryption;

import com.google.common.base.Preconditions;
import exception.EncryptionException;
import lombok.Getter;
import util.B64;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

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

        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = cipher.doFinal(messageBytes);
            return B64.encode(encryptedBytes);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    public static String decrypt(String message) {
        Preconditions.checkNotNull(privateKey, "Encryption keys not initialized");

        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] messageBytes = cipher.doFinal(B64.decode(message));
            return new String(messageBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }
}
