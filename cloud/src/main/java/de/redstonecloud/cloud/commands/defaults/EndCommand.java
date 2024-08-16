package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.logger.Logger;

public class EndCommand extends Command {
    public EndCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        Logger.getInstance().info("Stopping Cloud using command...");
        this.getServer().stop();
    }
}