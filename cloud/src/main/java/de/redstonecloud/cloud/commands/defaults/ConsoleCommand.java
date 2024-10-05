package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.logger.Logger;
import de.redstonecloud.cloud.server.Server;
import de.redstonecloud.cloud.server.ServerLogger;
import io.netty.util.internal.EmptyArrays;

public class ConsoleCommand extends Command {
    public int argCount = 1;

    public ConsoleCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length == 0) {
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
        return getServer().getServerManager().getServers().keySet().toArray(EmptyArrays.EMPTY_STRINGS);
    }
}