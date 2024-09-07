package de.redstonecloud.cloud.events.defaults;

import de.redstonecloud.cloud.events.Event;
import lombok.Getter;

public class ServerStartEvent extends Event {
    @Getter
    private String serverName;

    public ServerStartEvent(String name) {
        this.serverName = name;
    }
}