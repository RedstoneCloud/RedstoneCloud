package de.redstonecloud.api.components;

import de.redstonecloud.api.redis.broker.message.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

@Getter
@RequiredArgsConstructor
public enum Requests {
    PLAYER_GET("player:get", message -> {
        String uuid = message.getArguments()[0];
    }),
    PLAYER_DELETE("player:delete"),
    PLAYER_UPDATE("player:update"),
    SERVER_GET("server:get"),
    SERVER_DELETE("server:delete"),
    SERVER_UPDATE("server:update"),
    ;

    private final String key;
    private final Consumer<Message> handle;

    public Message create(Object... subargs) {
        return new Message.Builder()
                .setTo("cloud")
                .append(this.key)
                .append(subargs)
                .build();
    }
}
