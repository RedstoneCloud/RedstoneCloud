package de.redstonecloud.cloud.events.defaults;

import de.redstonecloud.cloud.events.Event;
import de.redstonecloud.cloud.player.CloudPlayer;
import de.redstonecloud.cloud.server.Server;
import lombok.Getter;

@Getter
public class PlayerConnectEvent extends Event {
    private final Server network;
    private final CloudPlayer player;

    public PlayerConnectEvent(CloudPlayer p, Server srv) {
        this.player = p;
        this.network = srv;
    }
}