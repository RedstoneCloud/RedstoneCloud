package de.redstonecloud.commands.defaults;

import de.redstonecloud.RedstoneCloud;
import de.redstonecloud.commands.Command;
import de.redstonecloud.logger.Logger;
import de.redstonecloud.server.Server;

public class InfoCommand extends Command {
    public int argCount = 1;

    public InfoCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if(args.length == 0) {
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
        Logger.getInstance().info("Server Type: " + server.getType());
        Logger.getInstance().info("Server Status: " + server.getStatus());
        Logger.getInstance().info("Server Port: " + server.getPort());
    }

    @Override
    public String[] getArgs() {
        return getServer().getServerManager().getServers().keySet().toArray(String[]::new);
    }
}