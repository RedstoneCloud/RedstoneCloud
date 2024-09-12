package de.redstonecloud.api.netty;

import de.pierreschwang.nettypacket.registry.IPacketRegistry;
import de.pierreschwang.nettypacket.registry.SimplePacketRegistry;
import de.redstonecloud.api.netty.packet.communication.ClientAuthPacket;
import de.redstonecloud.api.netty.packet.template.GetBestTemplatePacket;

public class NettyHelper {

    public static IPacketRegistry constructRegistry() {
        IPacketRegistry registry = new SimplePacketRegistry();

        try {
            registry.registerPacket(ClientAuthPacket.NETWORK_ID, ClientAuthPacket.class);
            registry.registerPacket(GetBestTemplatePacket.NETWORK_ID, GetBestTemplatePacket.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return registry;
    }
}
