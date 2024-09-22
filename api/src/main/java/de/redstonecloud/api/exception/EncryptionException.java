package de.redstonecloud.api.exception;

public class EncryptionException extends RuntimeException {
    public EncryptionException(Throwable throwable) {
        super(throwable);
    }
}
