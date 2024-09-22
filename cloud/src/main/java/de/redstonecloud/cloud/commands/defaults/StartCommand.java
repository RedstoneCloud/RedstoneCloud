package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.logger.Logger;
import de.redstonecloud.cloud.server.Template;

public class StartCommand extends Command {
    public int argCount = 1;

    public StartCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length == 0) {
            Logger.getInstance().error("Usage: start <template> [count]");
            return;
        }

        Template template = RedstoneCloud.getInstance().getServerManager().getTemplate(args[0]);
        if (template == null) {
            Logger.getInstance().error("Template not found.");
            return;
        }

        if (args.length == 2) {
            for (int i = 1; i < Integer.parseInt(args[1]); i++) {
                RedstoneCloud.getInstance().getServerManager().startServer(template);
            }
        }

        RedstoneCloud.getInstance().getServerManager().startServer(template);
        Logger.getInstance().info("Successfully started server using template " + template.getName());
    }

    @Override
    public String[] getArgs() {
        return getServer().getServerManager().getTemplates().keySet().toArray(String[]::new);
    }
}