package util;

import java.util.Base64;

public class B64 {

    public static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] decode(String str) {
        return Base64.getDecoder().decode(str);
    }
}
