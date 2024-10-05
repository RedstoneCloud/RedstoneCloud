package de.redstonecloud.cloud.commands;

import de.redstonecloud.cloud.RedstoneCloud;
import io.netty.util.internal.EmptyArrays;

public class Command {

    public String cmd;

    public int argCount = 0;

    public Command(String cmd) {
        this.cmd = cmd;
    }

    protected void onCommand(String[] args) {

    }

    public String getCommand() {
        return this.cmd;
    }


    public final RedstoneCloud getServer() {
        return RedstoneCloud.getInstance();
    }

    public String[] getArgs() {
        return EmptyArrays.EMPTY_STRINGS;
    }

}