package de.redstonecloud.api.netty.server;

import de.pierreschwang.nettypacket.Packet;
import de.pierreschwang.nettypacket.event.EventRegistry;
import de.pierreschwang.nettypacket.handler.PacketChannelInboundHandler;
import de.pierreschwang.nettypacket.handler.PacketDecoder;
import de.pierreschwang.nettypacket.handler.PacketEncoder;
import de.pierreschwang.nettypacket.registry.IPacketRegistry;
import de.pierreschwang.nettypacket.response.RespondingPacket;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Getter
@Setter
@Accessors(chain = true)
public class NettyServer extends ChannelInitializer<Channel> {
    protected final ServerBootstrap bootstrap;
    protected Channel serverChannel;

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
        try {
            ChannelFuture channelFuture = this.bootstrap.bind("127.0.0.1", this.port).sync();

            this.serverChannel = channelFuture.channel();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        try {
            this.serverChannel.close().sync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            this.boss.shutdownGracefully();
            this.worker.shutdownGracefully();
        }
    }

    @Override
    protected void initChannel(Channel channel) {
        channel.pipeline()
                .addLast(new PacketDecoder(this.packetRegistry),
                        new PacketEncoder(this.packetRegistry),
                        new PacketChannelInboundHandler(this.eventRegistry));
    }

    public Optional<Channel> getChannel(String clientId) {
        return Optional.ofNullable(this.channelCache.getOrDefault(clientId, null));
    }

    public Collection<Channel> getChannels(String idPattern) {
        Pattern pattern = Pattern.compile(idPattern.replace("*", ".*")
                .replace("?", "."));

        return this.channelCache.entrySet().stream()
                .filter(entry -> pattern.matcher(entry.getKey()).matches())
                .map(Map.Entry::getValue)
                .toList();
    }

    public void sendPacket(String clientId, Packet packet) {
        this.getChannel(clientId)
                .ifPresent(channel -> channel.writeAndFlush(packet));
    }

    public void sendPacketMulti(String idPattern, Packet packet) {
        this.getChannels(idPattern).forEach(channel -> channel.writeAndFlush(packet));
    }

    public <T extends Packet> void sendPacket(String clientId, Packet packet, Consumer<T> callback, Class<T> clazz) {
        RespondingPacket<T> respondingPacket = new RespondingPacket<>(packet, clazz, callback);

        this.getChannel(clientId).ifPresent(respondingPacket::send);
    }
}
