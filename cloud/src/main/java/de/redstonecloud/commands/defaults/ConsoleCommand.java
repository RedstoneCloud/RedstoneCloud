package de.redstonecloud.commands.defaults;

import de.redstonecloud.RedstoneCloud;
import de.redstonecloud.commands.Command;
import de.redstonecloud.logger.Logger;
import de.redstonecloud.server.Server;
import de.redstonecloud.server.ServerLogger;

public class ConsoleCommand extends Command {
    public int argCount = 1;

    public ConsoleCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if(args.length == 0) {
            Logger.getInstance().error("Usage: console <server>");
            return;
        }

        Server server = RedstoneCloud.getInstance().getServerManager().getServer(args[0]);
        if (server == null) {
            Logger.getInstance().error("Server not found.");
            return;
        }

        ServerLogger logger = server.getLogger();

        Logger.getInstance().info("Console set to " + server.getName());
        logger.enableConsoleLogging();
        RedstoneCloud.getInstance().setCurrentLogServer(logger);
    }

    @Override
    public String[] getArgs() {
        return getServer().getServerManager().getServers().keySet().toArray(String[]::new);
    }
}