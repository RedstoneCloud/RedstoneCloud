package de.redstonecloud.cloud.redis;

import com.google.common.net.HostAndPort;
import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.api.redis.broker.packet.Packet;
import de.redstonecloud.api.redis.broker.packet.defaults.communication.ClientAuthPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.player.PlayerConnectPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.player.PlayerDisconnectPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.server.ServerChangeStatusPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.template.BestTemplateResultPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.template.GetBestTemplatePacket;
import de.redstonecloud.api.redis.broker.packet.defaults.template.ServerStartedPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.template.StartServerPacket;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.events.defaults.PlayerConnectEvent;
import de.redstonecloud.cloud.events.defaults.PlayerDisconnectEvent;
import de.redstonecloud.cloud.events.defaults.PlayerTransferEvent;
import de.redstonecloud.cloud.events.defaults.ServerReadyEvent;
import de.redstonecloud.cloud.player.CloudPlayer;
import de.redstonecloud.cloud.player.PlayerManager;
import de.redstonecloud.shared.server.Server;
import de.redstonecloud.cloud.server.ServerManager;
import de.redstonecloud.shared.server.Template;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PacketHandler {

    public static void handle(Packet packet) {
        switch (packet) {
            case ClientAuthPacket pk -> on(pk);
            case PlayerConnectPacket pk -> on(pk);
            case PlayerDisconnectPacket pk -> on(pk);
            case ServerChangeStatusPacket pk -> on(pk);
            case GetBestTemplatePacket pk -> on(pk);
            case StartServerPacket pk -> on(pk);
            default -> {
            }
        }
    }

    private static void on(ClientAuthPacket packet) {
        String clientId = packet.getClientId();

        ServerManager serverManager = RedstoneCloud.getInstance().getServerManager();
        Server server = serverManager.getServer(clientId);

        if (server == null || server.getStatus() != ServerStatus.STARTING) return;
        server.setStatus(ServerStatus.RUNNING);
        log.info("{} is ready", server.getName());
        RedstoneCloud.getInstance().getEventManager().callEvent(new ServerReadyEvent(server));
    }

    private static void on(PlayerConnectPacket packet) {
        Server server = RedstoneCloud.getInstance().getServerManager().getServer(packet.getServer());
        CloudPlayer p = PlayerManager.getInstance().getPlayer(packet.getUuid());

        if (server != null) {
            if (p == null) {
                p = CloudPlayer.builder()
                        .name(packet.getPlayerName())
                        .uuid(packet.getUuid())
                        .address(HostAndPort.fromString(packet.getIpAddress()))
                        .build();

                PlayerManager.getInstance().addPlayer(p);
                RedstoneCloud.getInstance().getEventManager().callEvent(new PlayerConnectEvent(p, server));
            }

            if (server.getType().isProxy()) p.setConnectedNetwork(server);
            else {
                RedstoneCloud.getInstance().getEventManager().callEvent(new PlayerTransferEvent(p, (Server) p.getConnectedServer(), server));
                p.setConnectedServer(server);
            }

            log.info("Player " + p.getName() + " connected to " + server.getName());
        }
    }

    private static void on(PlayerDisconnectPacket packet) {
        Server server = RedstoneCloud.getInstance().getServerManager().getServer(packet.getServer());
        CloudPlayer p = PlayerManager.getInstance().getPlayer(packet.getUuid());
        if (p != null && server != null)
            log.info("Player " + p.getName() + " disconnected from " + server.getName());
        if (server != null && server.getType().isProxy()) {
            if (p != null) {
                RedstoneCloud.getInstance().getEventManager().callEvent(new PlayerDisconnectEvent(p, (Server) p.getConnectedNetwork(), (Server) p.getConnectedServer()));
                p.setConnectedServer(null);
                p.setConnectedNetwork(null);
            }

            PlayerManager.getInstance().removePlayer(packet.getUuid());
        }
    }

    private static void on(ServerChangeStatusPacket packet) {
        Server server = RedstoneCloud.getInstance().getServerManager().getServer(packet.getServer());
        if (server != null)
            server.setStatus(ServerStatus.valueOf(packet.getNewStatus()));
    }

    private static void on(GetBestTemplatePacket packet) {
        Template template = ServerManager.getInstance().getTemplate(packet.getTemplate());
        if (template != null) {
            ServerManager.BestServerResult[] s = ServerManager.getInstance().getBestServer(template);
            if (s.length == 0)
                return;

            ServerManager.BestServerResult ss = s[0];
            String name = ss.server().getName();

            new BestTemplateResultPacket(name)
                    .setTo(packet.getFrom())
                    .setSessionId(packet.getSessionId())
                    .send();
        }
    }

    private static void on(StartServerPacket packet) {
        Template t = ServerManager.getInstance().getTemplate(packet.getTemplate());
        Server s = ServerManager.getInstance().startServer(t);

        new ServerStartedPacket(s.getName())
                .setTo(packet.getFrom())
                .setSessionId(packet.getSessionId())
                .send();
    }
}
