package de.redstonecloud.cloud.server;

public record ServerType(String name, String[] startCommand, boolean isProxy, String logsPath, String portSettingFile,
                         String portSettingPlaceholder) {
}
