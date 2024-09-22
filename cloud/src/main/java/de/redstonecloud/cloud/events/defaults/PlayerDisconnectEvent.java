package de.redstonecloud.cloud.events.defaults;

import de.redstonecloud.cloud.events.Event;
import de.redstonecloud.cloud.player.CloudPlayer;
import de.redstonecloud.cloud.server.Server;
import lombok.Getter;

@Getter
public class PlayerDisconnectEvent extends Event {
    private final Server network;
    private final Server server;
    private final CloudPlayer player;

    public PlayerDisconnectEvent(CloudPlayer p, Server netw, Server srv) {
        this.player = p;
        this.network = netw;
        this.server = srv;
    }
}