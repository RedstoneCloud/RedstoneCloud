package netty.server;

import de.pierreschwang.nettypacket.event.EventRegistry;
import de.pierreschwang.nettypacket.handler.PacketChannelInboundHandler;
import de.pierreschwang.nettypacket.handler.PacketDecoder;
import de.pierreschwang.nettypacket.handler.PacketEncoder;
import de.pierreschwang.nettypacket.registry.IPacketRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class NettyServer extends ChannelInitializer<Channel> {
    protected final ServerBootstrap bootstrap;
    protected final IPacketRegistry packetRegistry;
    protected final EventRegistry eventRegistry;

    protected final Object2ObjectOpenHashMap<String, Channel> channelCache = new Object2ObjectOpenHashMap<>();

    protected EventLoopGroup boss = new NioEventLoopGroup();
    protected EventLoopGroup worker = new NioEventLoopGroup();

    protected int port;

    public NettyServer(IPacketRegistry packetRegistry, EventRegistry eventRegistry) {
        this.bootstrap = new ServerBootstrap()
                .option(ChannelOption.AUTO_READ, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .channel(NioServerSocketChannel.class)
                .group(this.boss, this.worker)
                .childHandler(this);

        this.packetRegistry = packetRegistry;
        this.eventRegistry = eventRegistry;
    }

    public void bind() {
        this.bootstrap.bind("127.0.0.1", this.port);
    }

    @Override
    protected void initChannel(Channel channel) {
        channel.pipeline()
                .addLast(new PacketDecoder(this.packetRegistry),
                        new PacketEncoder(this.packetRegistry),
                        new PacketChannelInboundHandler(this.eventRegistry));
    }
}
