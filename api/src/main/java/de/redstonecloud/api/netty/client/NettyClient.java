package de.redstonecloud.api.netty.client;

import de.pierreschwang.nettypacket.Packet;
import de.pierreschwang.nettypacket.event.EventRegistry;
import de.pierreschwang.nettypacket.handler.PacketChannelInboundHandler;
import de.pierreschwang.nettypacket.handler.PacketDecoder;
import de.pierreschwang.nettypacket.handler.PacketEncoder;
import de.pierreschwang.nettypacket.registry.IPacketRegistry;
import de.pierreschwang.nettypacket.response.RespondingPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import de.redstonecloud.api.netty.packet.communication.ClientAuthPacket;

import java.util.function.Consumer;

@Getter
@Setter
@Accessors(chain = true)
public class NettyClient extends ChannelInitializer<Channel> {
    protected final Bootstrap bootstrap;
    protected final String clientId;
    protected final IPacketRegistry packetRegistry;
    protected final EventRegistry eventRegistry;

    @Setter(AccessLevel.NONE)
    protected Channel channel;

    protected EventLoopGroup worker = new NioEventLoopGroup();

    protected int port;

    public NettyClient(String clientId, IPacketRegistry packetRegistry, EventRegistry eventRegistry) {
        this.bootstrap = new Bootstrap()
                .option(ChannelOption.AUTO_READ, true)
                .channel(NioSocketChannel.class)
                .group(this.worker)
                .handler(this);

        this.clientId = clientId;
        this.packetRegistry = packetRegistry;
        this.eventRegistry = eventRegistry;
    }

    public void bind() {
        this.bootstrap.connect("127.0.0.1", this.port);
    }

    @Override
    protected void initChannel(Channel channel) {
        channel.pipeline()
                .addLast(new PacketDecoder(this.packetRegistry),
                        new PacketEncoder(this.packetRegistry),
                        new PacketChannelInboundHandler(this.eventRegistry));

        this.channel = channel;

        this.sendPacket(new ClientAuthPacket(this.clientId));
    }

    public void sendPacket(Packet packet) {
        this.channel.writeAndFlush(packet);
    }

    public <T extends Packet> void sendPacket(Packet packet, Consumer<T> callback, Class<T> clazz) {
        RespondingPacket<T> respondingPacket = new RespondingPacket<>(packet, clazz, callback);
        respondingPacket.send(this.channel);
    }
}
