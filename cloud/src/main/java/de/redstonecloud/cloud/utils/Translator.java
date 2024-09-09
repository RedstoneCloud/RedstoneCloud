package de.redstonecloud.cloud.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.cloud.RedstoneCloud;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Translator {
    private static JsonObject langFile;

    static {
        try {
            langFile = new Gson().fromJson(Files.readString(Paths.get(RedstoneCloud.workingDir + "/language.json")), JsonObject.class);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load language files: " + e);
        }
    }

    public static String translate(String key) {
        return langFile.has(key) ? langFile.get(key).getAsString() : key;
    }

    public static String translate(String key, Object... replace) {
        if(!langFile.has(key)) return key;

        String value = langFile.get(key).getAsString();

        if (replace != null && replace.length > 0) {
            StringBuilder sb = new StringBuilder(value);
            String str = sb.toString();
            int i = 0;
            for (Object rep : replace) {
                /*int index = sb.indexOf("{" + i +"}");
                if (index != -1) {
                    sb.replace(index, index + 3, String.valueOf(rep));
                }*/
                str = str.replace("{"+i+"}", String.valueOf(rep));
                i++;
            }
            value = str;
        }

        return value;
    }

}
