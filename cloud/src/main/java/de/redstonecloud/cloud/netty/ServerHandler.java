package de.redstonecloud.cloud.netty;

import de.pierreschwang.nettypacket.event.PacketSubscriber;
import de.redstonecloud.api.netty.packet.server.ServerActionRequest;
import de.redstonecloud.api.netty.server.NettyServer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServerHandler {
    protected final NettyServer server;

    @PacketSubscriber
    public void on(ServerActionRequest packet) {
        server.sendPacket(packet.getServer().toUpperCase(), packet.toActionPacket());
    }
}
