package de.redstonecloud.commands.defaults;

import de.redstonecloud.commands.Command;
import de.redstonecloud.logger.Logger;

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