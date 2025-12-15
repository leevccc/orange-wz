package orange.wz.provider.properties;

import java.util.Map;

public enum WzExtendedType {
    LIST("Property"),
    CANVAS("Canvas"),
    VECTOR("Shape2D#Vector2D"),
    CONVEX("Shape2D#Convex2D"),
    SOUND("Sound_DX8"),
    UOL("UOL"),
    RAW_DATA("RawData");

    private static final Map<String, WzExtendedType> types = Map.of(
            LIST.getString(), LIST,
            CANVAS.getString(), CANVAS,
            VECTOR.getString(), VECTOR,
            CONVEX.getString(), CONVEX,
            SOUND.getString(), SOUND,
            UOL.getString(), UOL,
            RAW_DATA.getString(), RAW_DATA
    );
    private final String string;

    WzExtendedType(String string) {
        this.string = string;
    }

    public final String getString() {
        return string;
    }

    public static WzExtendedType getByString(String name) {
        if (!types.containsKey(name)) {
            throw new IllegalArgumentException("Unknown WzPropertyType : " + name);
        }
        return types.get(name);
    }
}
