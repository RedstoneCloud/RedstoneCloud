package netty;

import de.pierreschwang.nettypacket.registry.IPacketRegistry;
import de.pierreschwang.nettypacket.registry.SimplePacketRegistry;
import netty.packet.ClientAuthPacket;

public class NettyHelper {

    public static IPacketRegistry constructRegistry() {
        IPacketRegistry registry = new SimplePacketRegistry();

        try {
            registry.registerPacket(ClientAuthPacket.NETWORK_ID, ClientAuthPacket.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return registry;
    }
}
