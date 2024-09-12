package de.redstonecloud.cloud.events.defaults;

import de.redstonecloud.cloud.events.Event;
import lombok.Getter;

@Getter
public class ServerStartEvent extends Event {
    private final String serverName;

    public ServerStartEvent(String name) {
        this.serverName = name;
    }
}