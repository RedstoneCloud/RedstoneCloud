package netty.client;

import de.pierreschwang.nettypacket.Packet;
import de.pierreschwang.nettypacket.event.EventRegistry;
import de.pierreschwang.nettypacket.handler.PacketChannelInboundHandler;
import de.pierreschwang.nettypacket.handler.PacketDecoder;
import de.pierreschwang.nettypacket.handler.PacketEncoder;
import de.pierreschwang.nettypacket.registry.IPacketRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import netty.packet.ClientAuthPacket;

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

    protected int port;

    public NettyClient(String clientId, IPacketRegistry packetRegistry, EventRegistry eventRegistry) {
        this.bootstrap = new Bootstrap()
                .option(ChannelOption.AUTO_READ, true)
                .channel(NioSocketChannel.class)
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
}
