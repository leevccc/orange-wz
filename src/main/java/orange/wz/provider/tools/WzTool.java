package orange.wz.provider.tools;

import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class WzTool {
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

    /**
     * WzObject 按名称排序：类型 Folder > WzFile > WzDir > 名称 自然数 > 字母
     *
     * @param objects 要排序的 List
     */
    public static void sortWzObjects(List<? extends WzObject> objects) {
        List<WzType> typePriority = List.of(
                WzType.FOLDER,
                WzType.WZ_FILE,
                WzType.DIRECTORY
        );

        objects.sort(Comparator
                .comparing((WzObject node) -> {
                    int index = typePriority.indexOf(node.getType());
                    return index == -1 ? Integer.MAX_VALUE : index; // 未定义的type排最后
                })
                .thenComparing(WzObject::getName, (a, b) -> {
                    // 内联的自然排序比较器
                    if (a == null && b == null) return 0;
                    if (a == null) return -1;
                    if (b == null) return 1;

                    int aIndex = 0, bIndex = 0;
                    int aLength = a.length();
                    int bLength = b.length();

                    while (aIndex < aLength && bIndex < bLength) {
                        char aChar = a.charAt(aIndex);
                        char bChar = b.charAt(bIndex);

                        if (Character.isDigit(aChar) && Character.isDigit(bChar)) {
                            int aNumber = 0;
                            while (aIndex < aLength && Character.isDigit(a.charAt(aIndex))) {
                                aNumber = aNumber * 10 + (a.charAt(aIndex) - '0');
                                aIndex++;
                            }

                            int bNumber = 0;
                            while (bIndex < bLength && Character.isDigit(b.charAt(bIndex))) {
                                bNumber = bNumber * 10 + (b.charAt(bIndex) - '0');
                                bIndex++;
                            }

                            if (aNumber != bNumber) {
                                return Integer.compare(aNumber, bNumber);
                            }
                        } else {
                            int compare = Character.compare(Character.toLowerCase(aChar), Character.toLowerCase(bChar));
                            if (compare != 0) {
                                return compare;
                            }
                            aIndex++;
                            bIndex++;
                        }
                    }

                    return aLength - bLength;
                }));
    }

    /**
     * 对 List 按名称进行排序后，将整数类型的名称从0开的顺序重新命名
     * @param list 要处理的列表
     * @return 是否有修改过名称
     */
    public static boolean sortAndReindexChildren(List<WzImageProperty> list) {
        WzTool.sortWzObjects(list);

        int i = 0;
        boolean renamed = false;
        for (WzImageProperty property : list) {
            String name = property.getName();
            String newName = String.valueOf(i);
            if (StringTool.isInteger(name)) {
                if (!name.equals(newName)) {
                    property.setNameAnyway(newName);
                    property.setTempChanged(true);
                    property.getWzImage().setChanged(true);
                    renamed = true;
                }
                i++;
            }
        }

        return renamed;
    }
}
