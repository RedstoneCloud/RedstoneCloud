package de.redstonecloud.api.netty.server.handler;

import de.redstonecloud.api.netty.server.NettyServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

@RequiredArgsConstructor
public class ClientDisconnectHandler extends ChannelInboundHandlerAdapter {
    protected final NettyServer server;
    protected final String clientId;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.server.getChannelCache().remove(this.clientId);

        //System.out.println("[NETTY] Channel disconnected with clientId " + this.clientId);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.printf("[NETTY] Caught an exception from %s: %s%n", this.clientId, cause.getMessage());

        if (cause instanceof IOException) {
            System.err.printf("Channel %s closed%n", this.clientId);
            ctx.close();
        }
    }
}
