package netty.server.handler;

import de.pierreschwang.nettypacket.event.PacketSubscriber;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import netty.server.NettyServer;
import netty.packet.communication.ClientAuthPacket;

@RequiredArgsConstructor
public class NettyEventHandler {
    protected final NettyServer server;

    @PacketSubscriber
    public void on(ClientAuthPacket packet, ChannelHandlerContext ctx) {
        String clientId = packet.getClientId();
        Channel channel = ctx.channel();

        this.server.getChannelCache().put(clientId, channel);
        channel.pipeline().addLast(new ClientDisconnectHandler(this.server, clientId));
        System.out.println("[NETTY] Channel connected with clientId " + clientId);
    }
}
