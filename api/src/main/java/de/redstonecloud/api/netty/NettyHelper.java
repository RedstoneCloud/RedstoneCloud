package de.redstonecloud.api.netty;

import de.pierreschwang.nettypacket.registry.IPacketRegistry;
import de.pierreschwang.nettypacket.registry.SimplePacketRegistry;
import de.redstonecloud.api.netty.packet.communication.ClientAuthPacket;
import de.redstonecloud.api.netty.packet.player.PlayerConnectPacket;
import de.redstonecloud.api.netty.packet.player.PlayerDisconnectPacket;
import de.redstonecloud.api.netty.packet.template.*;

public class NettyHelper {

    public static IPacketRegistry constructRegistry() {
        IPacketRegistry registry = new SimplePacketRegistry();

        try {
            registry.registerPacket(ClientAuthPacket.NETWORK_ID, ClientAuthPacket.class);

            registry.registerPacket(GetBestTemplatePacket.NETWORK_ID, GetBestTemplatePacket.class);
            registry.registerPacket(BestTemplateResultPacket.NETWORK_ID, BestTemplateResultPacket.class);
            registry.registerPacket(StartServerPacket.NETWORK_ID, StartServerPacket.class);
            registry.registerPacket(ServerStartedPacket.NETWORK_ID, ServerStartedPacket.class);

            registry.registerPacket(PlayerConnectPacket.NETWORK_ID, PlayerConnectPacket.class);
            registry.registerPacket(PlayerDisconnectPacket.NETWORK_ID, PlayerDisconnectPacket.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return registry;
    }
}
