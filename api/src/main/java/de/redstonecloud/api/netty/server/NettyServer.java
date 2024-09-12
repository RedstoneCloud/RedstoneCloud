package de.redstonecloud.api.netty.server;

import de.pierreschwang.nettypacket.Packet;
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

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

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

    public void sendPacket(String clientId, Packet packet) {
        Optional.ofNullable(this.channelCache.getOrDefault(clientId, null))
                .ifPresent(channel -> channel.writeAndFlush(packet));
    }

    public void sendPacketMulti(String idPattern, Packet packet) {
        Pattern pattern = Pattern.compile(idPattern.replace("*", ".*")
                .replace("?", "."));

        this.channelCache.entrySet().stream()
                .filter(entry -> pattern.matcher(entry.getKey()).matches())
                .map(Map.Entry::getValue)
                .forEach(channel -> channel.writeAndFlush(packet));
    }
}
