package de.redstonecloud.shared.files.template;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
public class TemplateBehavior extends OkaeriConfig {
    @Comment("Max players per server")
    int maxPlayers = 20;

    @Comment("Minimum servers running idle")
    int minServers = 1;

    @Comment("Maximum servers running in total")
    int maxServers = 2;

    @Comment("Create a new server when any server reaches this fraction of maxPlayers")
    double preStartThreshold = 0.8;

    @Comment("Maximum boot time before boot is cancelled (in ms)")
    int bootMillis = 60000;

    @Comment("Maximum shutdown time before final kill (in ms)")
    int shutdownMillis = 5000;

    @Comment("Whether or not the server should stop automatically if empty")
    boolean autoStop = false;
}
