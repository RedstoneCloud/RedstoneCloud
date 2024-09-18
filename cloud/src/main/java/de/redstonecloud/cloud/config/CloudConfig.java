package de.redstonecloud.cloud.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.cloud.RedstoneCloud;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CloudConfig {
    private static JsonObject cfg;

    public static JsonObject getCfg() {
        return getCfg(false);
    }

    public static JsonObject getCfg(boolean reload) {
        if (reload || cfg == null) {
            try {
                cfg = new Gson().fromJson(Files.readString(Paths.get(RedstoneCloud.workingDir + "/cloud.json")), JsonObject.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return cfg;
    }
}
