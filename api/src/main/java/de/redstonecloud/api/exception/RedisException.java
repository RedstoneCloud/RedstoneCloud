package de.redstonecloud.api.exception;

public class RedisException extends RuntimeException {
    public RedisException(String message) {
        super(message);
    }
}
