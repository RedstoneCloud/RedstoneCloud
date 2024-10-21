package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.logger.Logger;
import de.redstonecloud.cloud.server.Server;
import io.netty.util.internal.EmptyArrays;

public class InfoCommand extends Command {
    public int argCount = 1;

    public InfoCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length == 0) {
            Logger.getInstance().error("Usage: info <server>");
            return;
        }

        Server server = RedstoneCloud.getInstance().getServerManager().getServer(args[0]);
        if (server == null) {
            Logger.getInstance().error("Server not found.");
            return;
        }

        Logger.getInstance().info("== SERVER INFO: " + server.getName() + " ==");
        Logger.getInstance().info("Server Name: " + server.getName());
        Logger.getInstance().info("Server Template: " + server.getTemplate().getName());
        Logger.getInstance().info("Server Type: " + server.getType().name());
        Logger.getInstance().info("Server Status: " + server.getStatus());
        Logger.getInstance().info("Server Port: " + server.getPort());
    }

    @Override
    public String[] getArgs() {
        return getServer().getServerManager().getServers().keySet().toArray(EmptyArrays.EMPTY_STRINGS);
    }
}