package de.redstonecloud.cloud.server;

public record ServerType(String name, String[] startCommand, String logsPath, String portSettingFile,
                         String portSettingPlaceholder) {
}
