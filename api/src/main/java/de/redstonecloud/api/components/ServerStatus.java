package de.redstonecloud.api.components;

import lombok.Getter;

@Getter
public enum ServerStatus {
    NONE(-1),
    PREPARED(0),
    STARTING(1),
    RUNNING(2),
    STOPPING(3),
    STOPPED(4),
    ERROR(5),
    IN_GAME(6),
    WAITING(7);

    private final int value;

    ServerStatus(int value) {
        this.value = value;
    }
}
