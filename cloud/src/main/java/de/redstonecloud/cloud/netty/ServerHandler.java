package de.redstonecloud.cloud.netty;

import de.pierreschwang.nettypacket.event.PacketSubscriber;
import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.api.netty.packet.server.ServerActionRequest;
import de.redstonecloud.api.netty.packet.server.ServerChangeStatusPacket;
import de.redstonecloud.api.netty.server.NettyServer;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.server.Server;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServerHandler {
    protected final NettyServer server;

    @PacketSubscriber
    public void on(ServerActionRequest packet) {
        server.sendPacket(packet.getServer().toUpperCase(), packet.toActionPacket());
    }

    @PacketSubscriber
    public void on(ServerChangeStatusPacket packet) {
        Server srv = RedstoneCloud.getInstance().getServerManager().getServer(packet.getServer());
        if(srv == null) return;

        srv.setStatus(ServerStatus.valueOf(packet.getNewStatus()));
    }
}
