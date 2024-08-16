package de.redstonecloud.api.components;

import de.redstonecloud.api.redis.broker.message.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Requestor {
    PLAYER_GET("player:get"),
    PLAYER_DELETE("player:delete"),
    PLAYER_UPDATE("player:update"),
    SERVER_GET("server:get"),
    SERVER_DELETE("server:delete"),
    SERVER_UPDATE("server:update"),
    ;

    private final String key;

    public Message create(Object... subargs) {
        return new Message.Builder()
                .setTo("cloud")
                .append(this.key)
                .append(subargs)
                .build();
    }
}
