package de.redstonecloud.commands.defaults;

import de.redstonecloud.RedstoneCloud;
import de.redstonecloud.commands.Command;
import de.redstonecloud.logger.Logger;
import de.redstonecloud.server.Template;

public class StartCommand extends Command {
    public int argCount = 1;

    public StartCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if(args.length == 0) {
            Logger.getInstance().error("Usage: start <template>");
            return;
        }

        Template template = RedstoneCloud.getInstance().getServerManager().getTemplate(args[0]);
        if (template == null) {
            Logger.getInstance().error("Template not found.");
            return;
        }

        RedstoneCloud.getInstance().getServerManager().startServer(template);
        Logger.getInstance().info("Successfully started server using template " + template.getName());
    }

    @Override
    public String[] getArgs() {
        return getServer().getServerManager().getTemplates().keySet().toArray(String[]::new);
    }
}