package de.redstonecloud.cloud.events.defaults;

import de.redstonecloud.cloud.events.Event;
import de.redstonecloud.cloud.server.Server;
import lombok.Getter;

@Getter
public class ServerExitEvent extends Event {
    private final Server server;

    public ServerExitEvent(Server srv) {
        this.server = srv;
    }
}