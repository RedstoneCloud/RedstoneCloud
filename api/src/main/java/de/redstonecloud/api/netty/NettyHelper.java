package de.redstonecloud.api.netty;

import de.pierreschwang.nettypacket.registry.IPacketRegistry;
import de.pierreschwang.nettypacket.registry.SimplePacketRegistry;
import de.redstonecloud.api.netty.packet.EncryptedPacket;
import de.redstonecloud.api.netty.packet.communication.ClientAuthPacket;
import de.redstonecloud.api.netty.packet.player.PlayerConnectPacket;
import de.redstonecloud.api.netty.packet.player.PlayerDisconnectPacket;
import de.redstonecloud.api.netty.packet.server.RemoveServerPacket;
import de.redstonecloud.api.netty.packet.server.ServerAction;
import de.redstonecloud.api.netty.packet.server.ServerActionRequest;
import de.redstonecloud.api.netty.packet.server.ServerChangeStatusPacket;
import de.redstonecloud.api.netty.packet.template.*;

import lombok.Getter;

public class NettyHelper {
    public static class Holder {
        @Getter private static IPacketRegistry registry;
    }

    public static IPacketRegistry constructRegistry() {
        if (Holder.registry != null) {
            return Holder.registry;
        }

        IPacketRegistry registry = new SimplePacketRegistry();
        Holder.registry = registry;

        try {
            registry.registerPacket(EncryptedPacket.NETWORK_ID, EncryptedPacket.class);
            registry.registerPacket(ClientAuthPacket.NETWORK_ID, ClientAuthPacket.class);

            registry.registerPacket(GetBestTemplatePacket.NETWORK_ID, GetBestTemplatePacket.class);
            registry.registerPacket(BestTemplateResultPacket.NETWORK_ID, BestTemplateResultPacket.class);
            registry.registerPacket(StartServerPacket.NETWORK_ID, StartServerPacket.class);
            registry.registerPacket(ServerStartedPacket.NETWORK_ID, ServerStartedPacket.class);

            registry.registerPacket(PlayerConnectPacket.NETWORK_ID, PlayerConnectPacket.class);
            registry.registerPacket(PlayerDisconnectPacket.NETWORK_ID, PlayerDisconnectPacket.class);

            registry.registerPacket(RemoveServerPacket.NETWORK_ID, RemoveServerPacket.class);
            registry.registerPacket(ServerAction.NETWORK_ID, ServerAction.class);
            registry.registerPacket(ServerActionRequest.NETWORK_ID, ServerActionRequest.class);
            registry.registerPacket(ServerChangeStatusPacket.NETWORK_ID, ServerChangeStatusPacket.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return registry;
    }
}
