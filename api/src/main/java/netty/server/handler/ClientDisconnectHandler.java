package netty.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import netty.server.NettyServer;

@RequiredArgsConstructor
public class ClientDisconnectHandler extends ChannelInboundHandlerAdapter {
    protected final NettyServer server;
    protected final String clientId;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.server.getChannelCache().remove(this.clientId);

        System.out.println("[NETTY] Channel disconnected with clientId " + this.clientId);
        super.channelInactive(ctx);
    }
}
