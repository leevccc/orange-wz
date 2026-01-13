package orange.wz.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.properties.*;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.WzChildrenProperty;
import orange.wz.provider.tools.WzType;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Setter
@Getter
public abstract class WzImageProperty extends WzObject {
    protected WzImage wzImage;
    protected WzChildrenProperty children;

    protected WzImageProperty(String name, WzType type, WzObject parent, WzImage wzImage) {
        super(name, type, parent);
        this.wzImage = wzImage;

        this.children = switch (type) {
            case FOLDER, WZ_FILE, DIRECTORY, IMAGE -> throw new RuntimeException("不可使用的属性: " + type);
            case CANVAS_PROPERTY, CONVEX_PROPERTY, LIST_PROPERTY, RAW_DATA_PROPERTY -> new WzChildrenProperty();
            case DOUBLE_PROPERTY, FLOAT_PROPERTY, INT_PROPERTY, LONG_PROPERTY, NULL_PROPERTY, PNG_PROPERTY,
                 SHORT_PROPERTY, SOUND_PROPERTY, STRING_PROPERTY, UOL_PROPERTY, VECTOR_PROPERTY -> null;
        };
    }

    // Children --------------------------------------------------------------------------------------------------------
    public WzImageProperty getChild(String name) {
        if (children == null) return null;
        return children.get(name);
    }

    public List<WzImageProperty> getChildren() {
        if (children == null) return null;
        return children.get();
    }

    public boolean addChild(WzImageProperty child) {
        if (children == null) return false;
        if (children.add(child)) {
            if (wzImage != null) { // deepClone 时wzImage为null
                wzImage.setChanged(true);
                wzImage.setTempChanged(true);
            }
            return true;
        }
        return false;
    }

    public void addChildren(List<WzImageProperty> children) {
        if (children == null) return;
        this.children.add(children);
    }

    public boolean removeChild(String name) {
        if (children == null) return false;
        if (children.remove(name)) {
            wzImage.setChanged(true);
            wzImage.setTempChanged(true);
            return true;
        }
        return false;
    }

    public boolean existChild(String name) {
        if (children == null) return false;
        return children.existChild(name);
    }

    /**
     * 判断是否是 List 节点: 指的是 children 对象不为 null 的节点，即可拥有子节点的对象。
     *
     * @return children != null
     */
    public boolean isListProperty() {
        return children != null;
    }

    public void setChildrenWzImage(WzImage wzImage) {
        if (this instanceof WzCanvasProperty canvas) {
            canvas.setWzImage(wzImage);
            return;
        } else if (children == null) return;

        for (WzImageProperty property : children.get()) {
            property.setWzImage(wzImage);
            property.setChildrenWzImage(wzImage);
        }
    }

    // 解析 -------------------------------------------------------------------------------------------------------------
    public static List<WzImageProperty> parsePropertyList(int offset, BinaryReader reader, WzObject parent) {
        WzImage wzImage = null;
        if (parent instanceof WzImage img) {
            wzImage = img;
        } else if (parent instanceof WzImageProperty prop) {
            wzImage = prop.getWzImage();
        }

        int entryCount = reader.readCompressedInt();
        List<WzImageProperty> properties = new ArrayList<>();
        for (int i = 0; i < entryCount; i++) {
            String name = reader.readStringBlock(offset);
            byte pType = reader.getByte();
            switch (pType) {
                case 0: // null
                    properties.add(new WzNullProperty(name, parent, wzImage));
                    break;
                case 2, 11: // short
                    properties.add(new WzShortProperty(name, reader.getShort(), parent, wzImage));
                    break;
                case 3, 19: // int
                    properties.add(new WzIntProperty(name, reader.readCompressedInt(), parent, wzImage));
                    break;
                case 20: // long
                    properties.add(new WzLongProperty(name, reader.readCompressedLong(), parent, wzImage));
                    break;
                case 4: // float
                    properties.add(new WzFloatProperty(name, reader.readCompressedFloat(), parent, wzImage));
                    break;
                case 5: // double
                    properties.add(new WzDoubleProperty(name, reader.getDouble(), parent, wzImage));
                    break;
                case 8: // string
                    properties.add(new WzStringProperty(name, reader.readStringBlock(offset), parent, wzImage));
                    break;
                case 9:
                    int eob = reader.getInt() + reader.getPosition();
                    WzImageProperty exProp = parseExtendedProp(reader, offset, eob, name, parent);
                    properties.add(exProp);
                    if (reader.getPosition() != eob) reader.setPosition(eob);
                    break;
                default:
                    log.error("Unknown property type at ParsePropertyList, pType = {}", pType);
            }
        }
        return properties;
    }

    private static WzExtended parseExtendedProp(BinaryReader reader, int offset, long endOfBlock, String name, WzObject parent) {
        try {
            final byte stringType = reader.getByte();
            return switch (stringType) {
                case 0x01, WzImage.withOffsetFlag ->
                        extractMore(reader, offset, endOfBlock, name, reader.readStringAtOffset(offset + reader.getInt()), parent);
                case 0x00, WzImage.withoutOffsetFlag -> extractMore(reader, offset, endOfBlock, name, "", parent);
                default -> throw new Exception("Invalid byte read at ParseExtendedProp");
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static WzExtended extractMore(BinaryReader reader, int offset, long eob, String name, String iname, WzObject parent) {
        WzImage wzImage = null;
        if (parent instanceof WzImage img) {
            wzImage = img;
        } else if (parent instanceof WzImageProperty prop) {
            wzImage = prop.getWzImage();
        }

        if (iname.equalsIgnoreCase("")) {
            iname = reader.readString();
        }
        final WzExtendedType propertyType = WzExtendedType.getByString(iname);
        try {
            return switch (propertyType) {
                case WzExtendedType.LIST -> {
                    WzListProperty subProp = new WzListProperty(name, parent, wzImage);
                    reader.skip(2); // Reserved?
                    subProp.addChildren(WzImageProperty.parsePropertyList(offset, reader, subProp));
                    yield subProp;
                }
                case WzExtendedType.CANVAS -> {
                    WzCanvasProperty canvasProp = new WzCanvasProperty(name, parent, wzImage);
                    reader.skip(1);
                    if (reader.getByte() == 1) {
                        reader.skip(2);
                        canvasProp.addChildren(WzImageProperty.parsePropertyList(offset, reader, canvasProp));
                    }
                    canvasProp.initPngProperty(name, canvasProp, wzImage, reader);
                    yield canvasProp;
                }
                case WzExtendedType.VECTOR -> {
                    int x = reader.readCompressedInt();
                    int y = reader.readCompressedInt();
                    yield new WzVectorProperty(name, x, y, parent, wzImage);
                }
                case WzExtendedType.CONVEX -> {
                    WzConvexProperty convexProp = new WzConvexProperty(name, parent, wzImage);
                    int convexEntryCount = reader.readCompressedInt();
                    for (int i = 0; i < convexEntryCount; i++) {
                        convexProp.addChild(parseExtendedProp(reader, offset, 0, name, convexProp));
                    }
                    yield convexProp;
                }
                case WzExtendedType.SOUND -> {
                    WzSoundProperty soundProp = new WzSoundProperty(name, parent, wzImage);
                    soundProp.setData(reader);
                    yield soundProp;
                }
                case WzExtendedType.UOL -> {
                    reader.skip(1);
                    String value = switch (reader.getByte()) {
                        case 0 -> reader.readString();
                        case 1 -> reader.readStringAtOffset(offset + reader.getInt());
                        default -> throw new Exception("Unsupported UOL type");
                    };

                    yield new WzUOLProperty(name, value, parent, wzImage);
                }
                case WzExtendedType.RAW_DATA -> {  // GMS v220++
                    // GMS v255+
                    // UI_000.wz\Login.img\RaceSelect_new\Back0\3\skeleton.skel
                    // UI_000.wz\Login.img\RaceSelect_new\Back0\16\skeleton.skel
                    // UI_000.wz\Login.img\RaceSelect_new\Back0\1005\Sia.skel
                    byte type = reader.getByte();
                    WzRawDataProperty rawData = new WzRawDataProperty(name, type, -1, parent, wzImage);

                    // type 0: do nothing
                    // type 1: similar to CanvasProperty that has binary data (the PNG) and sub properties (the origin, _hash, etc.)
                    if (type == 1) {
                        if (reader.getByte() == 1) {
                            reader.skip(2);
                            rawData.addChildren(parsePropertyList(offset, reader, rawData));
                        }
                    }
                    // all types: parse the binary data (similar to parse WzPngProperty for WzCanvasProperty)
                    int length = reader.readCompressedInt();
                    rawData.setLength(length);
                    rawData.setOffset(reader.getPosition());
                    yield rawData;
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void writeValue(BinaryWriter writer);

    public abstract WzImageProperty deepClone(WzObject parent);
}
