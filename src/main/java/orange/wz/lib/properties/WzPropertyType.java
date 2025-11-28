package orange.wz.lib.properties;

import java.util.Map;

public enum WzPropertyType {
    LIST("Property"),
    CANVAS("Canvas"),
    VECTOR("Shape2D#Vector2D"),
    CONVEX("Shape2D#Convex2D"),
    SOUND("Sound_DX8"),
    UOL("UOL"),
    RAW_DATA("RawData");

    private static final Map<String, WzPropertyType> types = Map.of(
            LIST.getString(), LIST,
            CANVAS.getString(), CANVAS,
            VECTOR.getString(), VECTOR,
            CONVEX.getString(), CONVEX,
            SOUND.getString(), SOUND,
            UOL.getString(), UOL,
            RAW_DATA.getString(), RAW_DATA
    );
    private final String string;

    WzPropertyType(String string) {
        this.string = string;
    }

    public final String getString() {
        return string;
    }

    public static WzPropertyType getByString(String name) {
        if (!types.containsKey(name)) {
            throw new IllegalArgumentException("Unknown WzPropertyType : " + name);
        }
        return types.get(name);
    }
}
