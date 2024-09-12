package de.redstonecloud.cloud.broker;

import de.redstonecloud.api.components.Request;
import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.api.redis.broker.message.Message;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.server.Server;
import de.redstonecloud.cloud.server.ServerManager;
import de.redstonecloud.cloud.server.Template;
import de.redstonecloud.cloud.utils.Translator;

public class BrokerHandler {

    public static void handle(Message message) {
        String[] args = message.getArguments();

        Request.retrieveRequest(args[0]).ifPresent(request -> request.handle(message));

        if (Request.retrieveRequest(args[0]).isEmpty()) {
            switch (args[0].toLowerCase()) {
                case "comm:login" -> {
                    ServerManager serverManager = RedstoneCloud.getInstance().getServerManager();
                    Server server = serverManager.getServer(message.getFrom());

                    if (server == null || server.getStatus() != ServerStatus.STARTING) return;
                    server.setStatus(ServerStatus.RUNNING);
                    RedstoneCloud.getLogger().info(Translator.translate("cloud.server.ready", message.getFrom()));
                }

                case "template:getbest" -> {
                    Template template = ServerManager.getInstance().getTemplate(args[1]);
                    if (template != null) {
                        ServerManager.BestServerResult[] s = ServerManager.getInstance().getBestServer(template);
                        ServerManager.BestServerResult ss = s[0];
                        String name = ss.server().name; //debug reasons, dont ask why

                        message.respond(name).send();
                    }
                }
            }
        }
    }
}
