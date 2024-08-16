package de.redstonecloud.server;

import lombok.Getter;

@Getter
public class ServerType {
    private final String name;
    private final String[] startCommand;
    private final String logsPath;
    private final String portSettingFile;
    private final String portSettingPlaceholder;

    public ServerType(String name, String[] startCommand, String logsPath, String portSettingFile, String portSettingPlaceholder) {
       this.name = name;
       this.startCommand = startCommand;
       this.logsPath = logsPath;
       this.portSettingFile = portSettingFile;
       this.portSettingPlaceholder = portSettingPlaceholder;
    }
}
