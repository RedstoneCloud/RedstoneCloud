package de.redstonecloud.api.netty;

import de.pierreschwang.nettypacket.registry.IPacketRegistry;
import de.pierreschwang.nettypacket.registry.SimplePacketRegistry;
import de.redstonecloud.api.netty.packet.communication.ClientAuthPacket;
import de.redstonecloud.api.netty.packet.template.BestTemplateResultPacket;
import de.redstonecloud.api.netty.packet.template.GetBestTemplatePacket;
import lombok.Getter;

public class NettyHelper {
    public static class Holder {
        @Getter
        private static IPacketRegistry registry;
    }

    public static IPacketRegistry constructRegistry() {
        if (Holder.registry != null) {
            return Holder.registry;
        }

        IPacketRegistry registry = new SimplePacketRegistry();
        Holder.registry = registry;

        try {
            registry.registerPacket(ClientAuthPacket.NETWORK_ID, ClientAuthPacket.class);
            registry.registerPacket(GetBestTemplatePacket.NETWORK_ID, GetBestTemplatePacket.class);
            registry.registerPacket(BestTemplateResultPacket.NETWORK_ID, BestTemplateResultPacket.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return registry;
    }
}
