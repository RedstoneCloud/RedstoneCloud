package de.redstonecloud.cloud.player;

import de.redstonecloud.api.redis.cache.Cache;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class PlayerManager {
    @Getter
    private static PlayerManager instance;
    @Getter
    public Map<String, CloudPlayer> players = new HashMap<>();
    @Getter
    public Map<String, CloudPlayer> playersByName = new HashMap<>();

    public PlayerManager() {
        instance = this;
    }

    public void addPlayer(CloudPlayer player) {
        players.put(player.getUUID(), player);
        playersByName.put(player.getName(), player);

        player.updateCache();
    }

    public void removePlayer(String uuid) {
        CloudPlayer p = players.get(uuid);
        players.remove(uuid);
        if(p != null) playersByName.remove(p.getName());
        p.resetCache();

    }

    public CloudPlayer getPlayer(String uuid) {
        return players.get(uuid);
    }
}