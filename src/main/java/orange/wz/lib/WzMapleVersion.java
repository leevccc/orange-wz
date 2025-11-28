package orange.wz.lib;

public enum WzMapleVersion {
    GMS(0),
    CMS(1),
    LATEST(2);

    private final int value;

    WzMapleVersion(int value) {
        this.value = value;
    }

    public final int getValue() {
        return value;
    }

    public static WzMapleVersion getByValue(int value) {
        for (WzMapleVersion type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }
}
