package de.redstonecloud.server;

import lombok.Getter;

@Getter
public class ServerType {
    private final String name;
    private final String[] startCommand;
    private final String logsPath;

    public ServerType(String name, String[] startCommand, String logsPath) {
       this.name = name;
       this.startCommand = startCommand;
       this.logsPath = logsPath;
    }
}
