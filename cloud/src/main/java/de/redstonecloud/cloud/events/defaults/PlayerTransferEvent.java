package de.redstonecloud.cloud.events.defaults;

import de.redstonecloud.cloud.events.Event;
import de.redstonecloud.cloud.player.CloudPlayer;
import de.redstonecloud.cloud.server.Server;
import lombok.Getter;

@Getter
public class PlayerTransferEvent extends Event {
    private final Server from;
    private final Server to;
    private final CloudPlayer player;

    public PlayerTransferEvent(CloudPlayer p, Server oldSrv, Server newSrv) {
        this.player = p;
        this.from = oldSrv;
        this.to = newSrv;
    }
}