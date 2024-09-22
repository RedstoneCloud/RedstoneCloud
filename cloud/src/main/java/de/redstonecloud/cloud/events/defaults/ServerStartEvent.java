package de.redstonecloud.cloud.events.defaults;

import de.redstonecloud.cloud.events.Event;
import de.redstonecloud.cloud.server.Server;
import lombok.Getter;

@Getter
public class ServerStartEvent extends Event {
    private final Server server;

    public ServerStartEvent(Server srv) {
        this.server = srv;
    }
}