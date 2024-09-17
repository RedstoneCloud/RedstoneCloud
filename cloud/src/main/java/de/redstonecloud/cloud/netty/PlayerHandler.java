package de.redstonecloud.cloud.netty;

import com.google.common.net.HostAndPort;
import de.pierreschwang.nettypacket.event.PacketSubscriber;
import de.redstonecloud.api.netty.packet.player.PlayerConnectPacket;
import de.redstonecloud.api.netty.packet.player.PlayerDisconnectPacket;
import de.redstonecloud.api.netty.server.NettyServer;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.player.CloudPlayer;
import de.redstonecloud.cloud.player.PlayerManager;
import de.redstonecloud.cloud.server.Server;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlayerHandler {
    protected final NettyServer server;

    @PacketSubscriber
    public void on(PlayerConnectPacket packet) {
        Server server = RedstoneCloud.getInstance().getServerManager().getServer(packet.getServer());
        CloudPlayer p = PlayerManager.getInstance().getPlayer(packet.getUuid());

        if (server != null) {
            if (p == null) {
                p = CloudPlayer.builder()
                        .name(packet.getPlayerName())
                        .uuid(packet.getUuid())
                        .address(HostAndPort.fromParts(packet.getIpAddress(), 1))
                        .build();

                PlayerManager.getInstance().addPlayer(p);
            }

            if(server.getType().isProxy()) p.setConnectedNetwork(server);
            else p.setConnectedServer(server);

            RedstoneCloud.getLogger().info("Player " + p.getName() + " switched to " + server.getName());
        }
    }

    @PacketSubscriber
    public void on(PlayerDisconnectPacket packet) {
        Server server = RedstoneCloud.getInstance().getServerManager().getServer(packet.getServer());
        if (server != null && server.getType().isProxy()) {
            CloudPlayer p = PlayerManager.getInstance().getPlayer(packet.getUuid());
            if (p != null) {
                p.setConnectedServer(null);
                p.setConnectedNetwork(null);
            }

            PlayerManager.getInstance().removePlayer(packet.getUuid());
        }
    }
}
