package de.redstonecloud.cloud.utils;

import de.redstonecloud.cloud.RedstoneCloud;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

public class Utils {
    public static String[] dropFirstString(String[] input) {
        String[] anstring = new String[input.length - 1];
        System.arraycopy(input, 1, anstring, 0, input.length - 1);
        return anstring;
    }
}
