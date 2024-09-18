package de.redstonecloud.cloud.netty;

import de.pierreschwang.nettypacket.event.PacketSubscriber;
import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.api.netty.server.NettyServer;
import de.redstonecloud.api.netty.server.handler.ClientDisconnectHandler;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.events.defaults.ServerReadyEvent;
import de.redstonecloud.cloud.server.Server;
import de.redstonecloud.cloud.server.ServerManager;
import de.redstonecloud.cloud.utils.Translator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import de.redstonecloud.api.netty.packet.communication.ClientAuthPacket;

@RequiredArgsConstructor
public class CommHandler {
    protected final NettyServer server;

    @PacketSubscriber
    public void on(ClientAuthPacket packet, ChannelHandlerContext ctx) {
        String clientId = packet.getClientId();
        Channel channel = ctx.channel();

        this.server.getChannelCache().put(clientId, channel);
        channel.pipeline().addLast(new ClientDisconnectHandler(this.server, clientId));

        ServerManager serverManager = RedstoneCloud.getInstance().getServerManager();
        Server server = serverManager.getServer(clientId);

        if (server == null || server.getStatus() != ServerStatus.STARTING) return;
        server.setStatus(ServerStatus.RUNNING);
        RedstoneCloud.getLogger().info(Translator.translate("cloud.server.ready", clientId));
        RedstoneCloud.getInstance().getEventManager().callEvent(new ServerReadyEvent(server));
    }
}
