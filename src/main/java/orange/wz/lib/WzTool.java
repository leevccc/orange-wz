package orange.wz.lib;

import java.util.Map;

public class WzTool {
    public static byte[] getIvByMapleVersion(WzMapleVersion ver) {
        return switch (ver) {
            case WzMapleVersion.CMS -> WzAESConstant.WZ_CMS_IV;
            case WzMapleVersion.GMS -> WzAESConstant.WZ_GMS_IV;
            default -> WzAESConstant.WZ_LATEST_IV;
        };
    }

    public static int getCompressedIntLength(int i) {
        if (i > 127 || i < -127)
            return 5;
        return 1;
    }

    public static int getWzObjectValueLength(String s, byte type, Map<String, Integer> tempStringCache) {
        String storeName = type + "_" + s;
        if (s.length() > 4 && tempStringCache.containsKey(storeName)) {
            return 5;
        } else {
            tempStringCache.put(storeName, 1);
            return 1 + getEncodedStringLength(s);
        }
    }

    public static int getEncodedStringLength(String s) {
        if (s == null || s.isEmpty()) {
            return 1;
        }

        boolean unicode = false;
        int length = s.length();

        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c > 255) {
                unicode = true;
                break;
            }
        }

        int prefixLength = length > (unicode ? 126 : 127) ? 5 : 1;
        int encodedLength = unicode ? length * 2 : length;

        return prefixLength + encodedLength;
    }
}
