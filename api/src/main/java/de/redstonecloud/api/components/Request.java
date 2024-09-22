package de.redstonecloud.api.components;

import com.google.gson.JsonObject;
import de.redstonecloud.api.redis.broker.message.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.Consumer;

@RequiredArgsConstructor
public enum Request {
    PLAYER_GET("player:get", message -> {
        String uuid = message.getArguments()[1];
        // TODO: respond with player object
    }),
    PLAYER_DELETE("player:delete", message -> {
        String uuid = message.getArguments()[1];
        // TODO: delete from map
    }),
    PLAYER_UPDATE("player:update", message -> {
        String uuid = message.getArguments()[1];
        JsonObject updatedValues = Message.GSON.fromJson(message.getArguments()[2], JsonObject.class);
        // TODO: update object
    }),
    SERVER_GET("server:get", message -> {
        String serverId = message.getArguments()[1];
        // TODO: respond with server object
    }),
    SERVER_DELETE("server:delete", message -> {
        String serverId = message.getArguments()[1];
        // TODO: delete from map
    }),
    SERVER_UPDATE("server:update", message -> {
        String serverId = message.getArguments()[1];
        JsonObject updatedValues = Message.GSON.fromJson(message.getArguments()[2], JsonObject.class);
        // TODO: update object
    }),
    ;

    @Getter
    private final String key;
    private final Consumer<Message> handle;

    public Message create(Object... subargs) {
        return new Message.Builder()
                .setTo("cloud")
                .append(this.key)
                .append(subargs)
                .build();
    }

    public void handle(Message message) {
        this.handle.accept(message);
    }

    static Collection<Request> VALUES = null;

    public static Optional<Request> retrieveRequest(String key) {
        if (VALUES == null) {
            VALUES = List.of(values());
        }

        return VALUES.stream()
                .filter(request -> request.getKey().equals(key))
                .findFirst();
    }
}
