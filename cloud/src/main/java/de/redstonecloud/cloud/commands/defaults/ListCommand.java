package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.server.Server;

import java.util.Comparator;

public class ListCommand extends Command {
    public int argCount = 0;

    public ListCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        System.out.format("+---------+---------+-----------+----------+%n");
        System.out.format("| Name    | Type    |  Status   | Port     |%n");
        System.out.format("+---------+---------+-----------+----------+%n");
        String leftAlignment = "| %-7s | %-7s | %-9s | %-7s |%n";
        for (Server server : RedstoneCloud.getInstance().getServerManager().getServers().values().stream().sorted(Comparator.comparing(a -> a.getName())).toList()) {
            System.out.format(leftAlignment, server.getName(), server.getType().name() != null ? server.getType().name() : "null", server.getStatus().name() != null ? server.getStatus().name() : "null", String.valueOf(server.getPort()));
            System.out.format("+---------+---------+-----------+----------+%n");
        }
    }
}