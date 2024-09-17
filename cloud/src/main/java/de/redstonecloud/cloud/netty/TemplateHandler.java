package de.redstonecloud.cloud.netty;

import de.pierreschwang.nettypacket.event.PacketSubscriber;
import de.pierreschwang.nettypacket.io.Responder;
import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.api.netty.packet.template.BestTemplateResultPacket;
import de.redstonecloud.api.netty.packet.template.GetBestTemplatePacket;
import de.redstonecloud.api.netty.packet.template.ServerStartedPacket;
import de.redstonecloud.api.netty.packet.template.StartServerPacket;
import de.redstonecloud.api.netty.server.NettyServer;
import de.redstonecloud.api.netty.server.handler.ClientDisconnectHandler;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.server.Server;
import de.redstonecloud.cloud.server.ServerManager;
import de.redstonecloud.cloud.server.Template;
import de.redstonecloud.cloud.utils.Translator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import de.redstonecloud.api.netty.packet.communication.ClientAuthPacket;

@RequiredArgsConstructor
public class TemplateHandler {
    protected final NettyServer server;

    @PacketSubscriber
    public void on(GetBestTemplatePacket packet, ChannelHandlerContext ctx, Responder responder) {
        Template template = ServerManager.getInstance().getTemplate(packet.getTemplate());
        if (template != null) {
            ServerManager.BestServerResult[] s = ServerManager.getInstance().getBestServer(template);
            ServerManager.BestServerResult ss = s[0];
            String name = ss.server().name; //debug reasons, dont ask why

            BestTemplateResultPacket resp = new BestTemplateResultPacket();
            resp.setServer(name);

            responder.respond(resp);
        }
    }

    @PacketSubscriber
    public void on(StartServerPacket packet, Responder r) {
        Template t = ServerManager.getInstance().getTemplate(packet.getTemplate());
        Server s = ServerManager.getInstance().startServer(t);
        r.respond(new ServerStartedPacket(s.name));
    }
}
