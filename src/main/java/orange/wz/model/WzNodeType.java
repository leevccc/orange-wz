package orange.wz.model;

import orange.wz.provider.WzDirectory;
import orange.wz.provider.WzFile;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzObject;
import orange.wz.provider.properties.*;

public enum WzNodeType {
    FOLDER,
    WZ,
    WZ_DIRECTORY,
    IMAGE,
    IMAGE_CANVAS,
    IMAGE_CONVEX,
    IMAGE_DOUBLE,
    IMAGE_FLOAT,
    IMAGE_INT,
    IMAGE_LIST,
    IMAGE_LONG,
    IMAGE_NULL,
    IMAGE_SHORT,
    IMAGE_SOUND,
    IMAGE_STRING,
    IMAGE_UOL,
    IMAGE_RAW_DATA,
    IMAGE_VECTOR;

    public static WzNodeType getByWzObjectType(WzObject wzObject) {
        return switch (wzObject) {
            case WzFile ignored -> WzNodeType.WZ;
            case WzDirectory ignored -> WzNodeType.WZ_DIRECTORY;
            case WzImage ignored -> WzNodeType.IMAGE;
            case WzCanvasProperty ignored -> WzNodeType.IMAGE_CANVAS;
            case WzConvexProperty ignored -> WzNodeType.IMAGE_CONVEX;
            case WzDoubleProperty ignored -> WzNodeType.IMAGE_DOUBLE;
            case WzFloatProperty ignored -> WzNodeType.IMAGE_FLOAT;
            case WzIntProperty ignored -> WzNodeType.IMAGE_INT;
            case WzListProperty ignored -> WzNodeType.IMAGE_LIST;
            case WzLongProperty ignored -> WzNodeType.IMAGE_LONG;
            case WzNullProperty ignored -> WzNodeType.IMAGE_NULL;
            case WzShortProperty ignored -> WzNodeType.IMAGE_SHORT;
            case WzSoundProperty ignored -> WzNodeType.IMAGE_SOUND;
            case WzStringProperty ignored -> WzNodeType.IMAGE_STRING;
            case WzUOLProperty ignored -> WzNodeType.IMAGE_UOL;
            case WzVectorProperty ignored -> WzNodeType.IMAGE_VECTOR;
            case WzRawDataProperty ignored -> WzNodeType.IMAGE_RAW_DATA;
            case null, default -> null;
        };
    }
}
