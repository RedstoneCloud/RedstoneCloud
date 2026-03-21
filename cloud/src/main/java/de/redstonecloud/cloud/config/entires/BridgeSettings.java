package de.redstonecloud.cloud.config.entires;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
public class BridgeSettings extends OkaeriConfig {
    @Comment("Hub Template")
    String hubTemplate = "Lobby";

    @Comment("Description of the /hub command")
    String hubDescription = "Go back to the lobby server";

    @Comment("Message if no hub is available at the moment")
    String hubNotAvailable = "There is no hub server available at the moment.";

    @Comment("Name of an always running fallback server, leave empty if none")
    String fallbackServer = null;

    @Comment("Allow player to join fallback server on join if no other hub server is available")
    boolean fallbackOnJoin = true;
}