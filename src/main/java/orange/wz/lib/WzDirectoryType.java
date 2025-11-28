package orange.wz.lib;

public enum WzDirectoryType {
    UnknownType_1((byte) 1),
    RetrieveStringFromOffset_2((byte) 2),
    WzDirectory_3((byte) 3),
    WzImage_4((byte) 4);

    private final byte value;

    WzDirectoryType(byte value) {
        this.value = value;
    }

    public final byte getValue() {
        return value;
    }

    public static WzDirectoryType getByValue(byte value) {
        for (WzDirectoryType type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }
}
